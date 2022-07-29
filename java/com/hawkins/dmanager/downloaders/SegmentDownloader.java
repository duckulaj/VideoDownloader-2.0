package com.hawkins.dmanager.downloaders;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;

import com.hawkins.dmanager.Config;
import com.hawkins.dmanager.DManagerConstants;
import com.hawkins.dmanager.downloaders.metadata.DashMetadata;
import com.hawkins.dmanager.util.FormatUtilities;
import com.hawkins.dmanager.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SegmentDownloader extends Downloader implements SegmentListener {
	

	private boolean init = false;
	private int minChunkSize = 256 * 1024;
	private boolean assembleFinished;
	private long totalAssembled;

	protected SegmentDownloader(String id, String folder) {
		this.id = id;
		this.folder = new File(folder, id).getAbsolutePath();
		this.length = -1;
		this.maxCount = Config.getInstance().getMaxSegments();
		this.minChunkSize = Config.getInstance().getMinSegmentSize();
		this.lastDownloaded = downloaded;
		this.prevTime = System.currentTimeMillis();
		this.eta = "---";
	}

	public void start() {
		
		if (log.isDebugEnabled()) {
			log.debug("creating folder {}", folder);
		}
		
		
		new File(folder).mkdirs();
		chunks = new LinkedList<Segment>();
		try {
			Segment c1 = new SegmentImpl(this, folder);
			// handle case of single dash stream
			if (getMetadata() instanceof DashMetadata) {
				c1.setTag("T1");
			}
			c1.setLength(-1);
			c1.setStartOffset(0);
			c1.setDownloaded(0);
			chunks.add(c1);
			c1.download(this);
		} catch (IOException e) {
			this.errorCode = DManagerConstants.RESUME_FAILED;
			this.listener.downloadFailed(id);
		}

	}

	@Override
	public void resume() {
		try {
			stopFlag = false;
			log.info("Resuming");
			if (!restoreState()) {
				log.info("Starting from beginning");
				start();
				return;
			}
			this.lastDownloaded = downloaded;
			this.prevTime = System.currentTimeMillis();
			log.info("Restore success");
			init = true;
			Segment c1 = findInactiveChunk();
			if (c1 != null) {
				try {
					c1.download(this);
				} catch (Exception e) {
					log.info(e.getMessage());
					if (!stopFlag) {
						log.info(e.getMessage());
						this.errorCode = DManagerConstants.RESUME_FAILED;
						listener.downloadFailed(this.id);
						return;
					}
				}
			} else if (allFinished()) {
				assembleAsync();
			} else {
				log.info("Internal error: no inactive/incomplete chunk found while resuming!");
			}
		} catch (Exception e) {
			log.info(e.getMessage());
			this.errorCode = DManagerConstants.RESUME_FAILED;
			listener.downloadFailed(this.id);
			return;
		}
	}

	private synchronized void createChunk() throws IOException {
		if (stopFlag)
			return;
		int activeCount = getActiveChunkCount();
		if (log.isDebugEnabled()) {
			log.debug("active count: {}", activeCount);
		}
		if (activeCount == maxCount) {
			return;
		}

		int rem = maxCount - activeCount;

		rem -= retryFailedChunks(rem);

		if (rem > 0) {
			Segment c1 = findMaxChunk();
			Segment c = splitChunk(c1);
			if (c != null) {
				if (log.isDebugEnabled()) {
					log.debug("creating chunk {}", c);
				}
				chunks.add(c);
				c.download(this);
			}
		}
	}

	private Segment findMaxChunk() {
		if (stopFlag)
			return null;
		long size = -1;
		String id = null;
		for (int i = 0; i < chunks.size(); i++) {
			Segment c = chunks.get(i);
			if (c.isActive()) {
				long rem = c.getLength() - c.getDownloaded();
				if (rem > size) {
					id = c.getId();
					size = rem;
				}
			}
		}
		if (size < minChunkSize)
			return null;
		return getById(id);
	}

	// merge c2 into c1
	private void mergeChunk(Segment c1, Segment c2) {
		c1.setLength(c1.getLength() + c2.getLength());
	}

	private Segment splitChunk(Segment c) throws IOException {
		if (c == null || stopFlag)
			return null;
		long rem = c.getLength() - c.getDownloaded();
		long offset = c.getStartOffset() + c.getLength() - rem / 2;
		long len = rem / 2;
		if (log.isDebugEnabled()) {
			log.debug("Changing length from: {} to {}", c.getLength(), (c.getLength() - rem / 2));
		}
		c.setLength(c.getLength() - rem / 2);
		Segment c2 = new SegmentImpl(this, folder);
		// handle case of single dash stream
		if (getMetadata() instanceof DashMetadata) {
			c2.setTag("T1");
		}
		c2.setLength(len);
		c2.setStartOffset(offset);
		return c2;
	}

	private Segment findNextNeedyChunk(Segment chunk) {
		if (stopFlag)
			return null;
		long offset = chunk.getStartOffset() + chunk.getLength();
		for (int i = 0; i < chunks.size(); i++) {
			Segment c = chunks.get(i);
			if (c.getDownloaded() == 0 && !c.isFinished() && c.getStartOffset() == offset) {
				return c;
			}
		}
			
		return null;
	}

	private synchronized boolean onComplete(String id) throws IOException {
		if (allFinished() || length < 0) {
			// finish
			finished = true;
			updateStatus();
			try {
				assemble();
				log.info("********Download finished*********");
				updateStatus();
				listener.downloadFinished(this.id);
			} catch (Exception e) {
				if (!stopFlag) {
					log.info(e.getMessage());
					this.errorCode = DManagerConstants.ERR_ASM_FAILED;
					listener.downloadFailed(this.id);
					log.info("********Download failed*********");
				}
			}

			listener = null;
			return true;
		}
		Segment chunk = getById(id);
		if (log.isDebugEnabled()) {
			log.debug("Complete: {} {} {}", chunk, chunk.getDownloaded(), chunk.getLength());
		}
			Segment nextNeedyChunk = findNextNeedyChunk(chunk);
		if (nextNeedyChunk != null) {
			if (log.isDebugEnabled()) {
				log.debug("****************Needy chunk found****************");
				log.debug("Stopping: {}", nextNeedyChunk);
			}
			nextNeedyChunk.stop();
			chunks.remove(nextNeedyChunk);
			nextNeedyChunk.dispose();
			mergeChunk(chunk, nextNeedyChunk);
			createChunk();
			return false;
		}
		createChunk();
		return true;
	}

	@Override
	public synchronized void chunkInitiated(String id) throws IOException {
		if (stopFlag)
			return;
		if (!init) {
			Segment c = getById(id);
			this.length = c.getLength();
			init = true;
			if (log.isDebugEnabled()) {
				log.debug("size: {}", this.length);
			}
			super.getLastModifiedDate(c);
			saveState();
			chunkConfirmed(c);
			listener.downloadConfirmed(this.id);
		}
		if (length > 0) {
			createChunk();
		}
	}

	@Override
	public synchronized boolean chunkComplete(String id) throws IOException {
		if (finished) {
			return true;
		}

		if (stopFlag) {
			return true;
		}

		saveState();

		return onComplete(id);
	}

	@Override
	public void chunkUpdated(String id) {
		if (stopFlag)
			return;
		long now = System.currentTimeMillis();
		if (now - lastSaved > 5000) {
			synchronized (this) {
				saveState();
			}
			lastSaved = now;
		}
		if (now - lastUpdated > 1000) {
			updateStatus();
			lastUpdated = now;
			synchronized (this) {
				int activeCount = getActiveChunkCount();
				if (activeCount < maxCount) {
					int rem = maxCount - activeCount;
					try {
						retryFailedChunks(rem);
					} catch (Exception e) {
						log.info(e.getMessage());
					}
				}
			}
		}
	}

	private void assemble() throws IOException {
		InputStream in = null;
		FileOutputStream out = null;
		BufferedOutputStream bufOut = null;
		totalAssembled = 0L;
		assembling = true;
		assembleFinished = false;
		File outFile = new File(getOutputFolder(), getOutputFileName(true));
		try {
			if (stopFlag)
				return;
			byte[] buf = new byte[8192 * 8];
			log.info("assembling... ");
			Collections.sort(chunks, new SegmentComparator());

			out = new FileOutputStream(outFile);
			bufOut = new BufferedOutputStream(out);
			
			for (int i = 0; i < chunks.size(); i++) {
				if (log.isDebugEnabled()) { 
					log.debug("chunk {} {}", i, stopFlag);
				}
				Segment c = chunks.get(i);
				in = new FileInputStream(new File(folder, c.getId()));
				long rem = c.getLength();
				while (true) {
					int x = 0;
					if (rem > 0) {
						if (rem > buf.length) {
							x = buf.length;
						} else {
							x = (int) rem;
						}
					} else {
						x = buf.length;
					}
						
					
					// int x_original = (int) (rem > 0 ? (rem > buf.length ? buf.length : rem) : buf.length);
					
					int r = in.read(buf, 0, x);
					if (stopFlag) {
						return;
					}

					if (r == -1) {
						if (length > 0) {
							throw new IllegalArgumentException("Assemble EOF");
						} else {
							break;
						}
					}

					// out.write(buf, 0, r);
					bufOut.write(buf, 0, r);
					
					if (stopFlag) {
						return;
					}
					if (length > 0) {
						rem -= r;
						if (rem == 0)
							break;
					}
					totalAssembled += r;
					long now = System.currentTimeMillis();
					if (now - lastUpdated > 1000) {
						updateStatus();
						lastUpdated = now;
					}
				}
				in.close();
			}
			bufOut.flush();
			bufOut.close();
			out.close();
			setLastModifiedDate(outFile);
			assembleFinished = true;
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new IOException(e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e2) {
					log.info(e2.getMessage());
				}
			}
			if (bufOut != null) {
				try {
					bufOut.flush();
					bufOut.close();
					out.close();
				} catch (Exception e2) {
					log.info(e2.getMessage());
				}
			}
		}
	}

	// @Override
	// public abstract AbstractChannel createChannel(Segment segment);

	public void stop() {
		stopFlag = true;
		saveState();
		for (int i = 0; i < chunks.size(); i++) {
			chunks.get(i).stop();
		}
		listener.downloadStopped(id);
		listener = null;
	}

	private void saveState() {
		if (length < 0)
			return;
		StringBuilder sb = new StringBuilder();
		sb.append(this.length + "\n");
		sb.append(downloaded + "\n");
		sb.append(chunks.size() + "\n");
		for (int i = 0; i < chunks.size(); i++) {
			Segment seg = chunks.get(i);
			sb.append(seg.getId() + "\n");
			sb.append(seg.getLength() + "\n");
			sb.append(seg.getStartOffset() + "\n");
			sb.append(seg.getDownloaded() + "\n");
		}
		if (!StringUtils.isNullOrEmptyOrBlank(lastModified)) {
			sb.append(this.lastModified + "\n");
		}
		try (BufferedWriter bwr = new BufferedWriter(new FileWriter(new File(folder, "state.txt")))){
			
			
			//write contents of StringBuffer to a file
			bwr.write(sb.toString());
			bwr.flush();
			bwr.close();
			
		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}

	private boolean restoreState() {

		chunks = new LinkedList<Segment>();
		File file = new File(folder, "state.txt");
		if (!file.exists()) {
			file = getBackupFile(folder);
			if (file == null) {
				return false;
			}
		}
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {

			this.length = Long.parseLong(br.readLine());
			this.downloaded = Long.parseLong(br.readLine());
			int chunkCount = Integer.parseInt(br.readLine());
			for (int i = 0; i < chunkCount; i++) {
				String cid = br.readLine();
				long len = Long.parseLong(br.readLine());
				long off = Long.parseLong(br.readLine());
				long dwn = Long.parseLong(br.readLine());
				Segment seg = new SegmentImpl(folder, cid, off, len, dwn);
				// handle case of single dash stream
				if (getMetadata() instanceof DashMetadata) {
					seg.setTag("T1");
				}

				log.debug("id: " + seg.getId() + "\nlength: " + seg.getLength() + "\noffset: " + seg.getStartOffset()
						+ "\ndownload: " + seg.getDownloaded());
				chunks.add(seg);
			}
			this.lastModified = br.readLine();
			return true;
		} catch (Exception e) {
			log.info("Failed to load saved state");
			log.info(e.getMessage());
		}
		return false;
	}

	protected abstract void chunkConfirmed(Segment c);

	public boolean shouldCleanup() {
		return assembleFinished;
	}

	private void updateStatus() {
		try {
			long now = System.currentTimeMillis();
			if (this.assembling) {
				long len = length > 0 ? length : downloaded;
				progress = (int) ((totalAssembled * 100) / len);
			} else {
				long downloaded2 = 0;
				if (segDet == null) {
					segDet = new SegmentDetails();
				}
				if (segDet.getCapacity() < chunks.size()) {
					segDet.extend(chunks.size() - segDet.getCapacity());
				}
				segDet.setChunkCount(chunks.size());
				downloadSpeed = 0;
				for (int i = 0; i < chunks.size(); i++) {
					Segment s = chunks.get(i);
					downloaded2 += s.getDownloaded();
					SegmentInfo info = segDet.getChunkUpdates().get(i);
					info.setDownloaded(s.getDownloaded());
					info.setStart(s.getStartOffset());
					info.setLength(s.getLength());
					downloadSpeed += s.getTransferRate();
				}
				this.downloaded = downloaded2;
				if (length > 0) {
					progress = (int) ((downloaded * 100) / length);
					long diff = downloaded - lastDownloaded;
					long timeSpend = now - prevTime;
					if (timeSpend > 0) {
						float rate = ((float) diff / timeSpend) * 1000;
						if (rate > downloadSpeed) {
							downloadSpeed = rate;
						}
						this.eta = FormatUtilities.getETA(length - downloaded, rate);
						if (this.eta == null) {
							this.eta = "---";
						}
						lastDownloaded = downloaded;
						prevTime = now;
					}
				}
			}
			listener.downloadUpdated(id);
		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}

	private void assembleAsync() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				finished = true;
				try {
					assemble();
					log.info("********Download finished*********");
					updateStatus();
					cleanup();
					listener.downloadFinished(id);
				} catch (Exception e) {
					if (!stopFlag) {
						log.info(e.getMessage());
						errorCode = DManagerConstants.ERR_ASM_FAILED;
						listener.downloadFailed(id);
					}
				}
			}
		}).start();
	}

}
