package com.hawkins.dmanager.downloaders.hds;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;

import com.hawkins.dmanager.Config;
import com.hawkins.dmanager.DManagerConstants;
import com.hawkins.dmanager.downloaders.AbstractChannel;
import com.hawkins.dmanager.downloaders.Downloader;
import com.hawkins.dmanager.downloaders.Segment;
import com.hawkins.dmanager.downloaders.SegmentDetails;
import com.hawkins.dmanager.downloaders.SegmentImpl;
import com.hawkins.dmanager.downloaders.SegmentInfo;
import com.hawkins.dmanager.downloaders.SegmentListener;
import com.hawkins.dmanager.downloaders.http.HttpChannel;
import com.hawkins.dmanager.downloaders.metadata.HdsMetadata;
import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;
import com.hawkins.dmanager.downloaders.metadata.manifests.F4MManifest;
import com.hawkins.dmanager.mediaconversion.FFmpeg;
import com.hawkins.dmanager.mediaconversion.MediaConversionListener;
import com.hawkins.dmanager.util.DManagerUtils;
import com.hawkins.dmanager.util.FormatUtilities;
import com.hawkins.dmanager.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HdsDownloader extends Downloader implements SegmentListener, MediaConversionListener {
	

	private HdsMetadata metadata;
	private LinkedList<String> urlList;
	private Segment manifestSegment;
	private long totalAssembled;
	private String newFileName;
	private boolean assembleFinished;
	private FFmpeg ffmpeg;
	private int lastProgress;
	private float totalDuration;

	private final byte[] flv_sig = { (byte) 'F', (byte) 'L', (byte) 'V', 0x01, 0x05, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00,
			0x00, 0x00 };

	public HdsDownloader(String id, String folder, HdsMetadata metadata) {
		this.id = id;
		this.folder = new File(folder, id).getAbsolutePath();
		this.length = -1;
		this.metadata = metadata;
		this.maxCount = Config.getInstance().getMaxSegments();
		urlList = new LinkedList<String>();
		chunks = new LinkedList<Segment>();
		this.eta = "---";
	}

	public void start() {
		log.info("creating folder " + folder);
		new File(folder).mkdirs();

		this.lastDownloaded = downloaded;
		this.prevTime = System.currentTimeMillis();
		try {
			manifestSegment = new SegmentImpl(this, folder);
			manifestSegment.setTag("MF");
			manifestSegment.setLength(-1);
			manifestSegment.setStartOffset(0);
			manifestSegment.setDownloaded(0);
			manifestSegment.setTag("HLS");
			manifestSegment.download(this);
		} catch (IOException e) {
			this.errorCode = DManagerConstants.RESUME_FAILED;
			this.listener.downloadFailed(id);
		}
	}

	@Override
	public void chunkInitiated(String id) {
		if (!id.equals(manifestSegment.getId())) {
			processSegments();
		} else {
			isJavaClientRequired = ((HttpChannel) manifestSegment.getChannel()).isJavaClientRequired();
			super.getLastModifiedDate(manifestSegment);
		}
	}

	@Override
	public boolean chunkComplete(String id) {
		if (finished) {
			return true;
		}

		if (stopFlag) {
			return true;
		}
		if (id.equals(manifestSegment.getId())) {
			if (initOrUpdateSegments()) {
				listener.downloadConfirmed(this.id);
			} else {
				if (!stopFlag) {
					this.errorCode = DManagerConstants.ERR_INVALID_RESP;
					listener.downloadFailed(this.id);
					return true;
				}
			}
		} else {
			Segment s = getById(id);
			if (s.getLength() < 0) {
				s.setLength(s.getDownloaded());
			}

			if (allFinished()) {

				finished = true;
				long len = 0L;
				for (Segment ss : chunks) {
					len += ss.getLength();
				}
				if (len > 0) {
					this.length = len;
				}

				saveState();

				updateStatus();

				try {
					assembleFinished = false;
					assemble();
					log.info("********Download finished*********");
					updateStatus();
					assembleFinished = true;
					listener.downloadFinished(this.id);
				} catch (Exception e) {
					if (!stopFlag) {
						log.info(e.getMessage());
						this.errorCode = DManagerConstants.ERR_ASM_FAILED;
						listener.downloadFailed(this.id);
					}
				}
				listener = null;
				return true;
			}
		}

		processSegments();
		return true;
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
				processSegments();
			}
		}
	}

	@Override
	public AbstractChannel createChannel(Segment segment) {
		for (int i = 0; i < chunks.size(); i++) {
			if (segment == chunks.get(i)) {
				HdsMetadata md = new HdsMetadata();
				md.setUrl(urlList.get(i));
				md.setHeaders(metadata.getHeaders());
				return new HttpChannel(segment, md.getUrl(), md.getHeaders(), -1, isJavaClientRequired);
			}
		}
		log.info("Create manifest channel");
		return new HttpChannel(segment, metadata.getUrl(), metadata.getHeaders(), -1, isJavaClientRequired);
	}

	@Override
	public boolean shouldCleanup() {
		return assembleFinished;
	}

	private boolean initOrUpdateSegments() {
		try {
			F4MManifest mf = new F4MManifest(metadata.getUrl(),
					new File(folder, manifestSegment.getId()).getAbsolutePath());
			mf.setSelectedBitRate(metadata.getBitRate());
			this.totalDuration = mf.getDuration();
			log.info("Total duration " + totalDuration);
			ArrayList<String> urls = mf.getMediaUrls();
			if (urls.size() < 1) {
				log.info("Manifest contains no media");
				return false;
			}
			if (urlList.size() > 0 && urlList.size() != urls.size()) {
				log.info("Manifest media count mismatch- expected: " + urlList.size() + " got: " + urls.size());
				return false;
			}
			if (urlList.size() > 0) {
				urlList.clear();
			}
			urlList.addAll(urls);

			String newExtension = null;

			if (chunks.size() < 1) {
				for (int i = 0; i < urlList.size(); i++) {
					if (newExtension == null && outputFormat == 0) {
						newExtension = findExtension(urlList.get(i));
						log.info("HDS: found new extension: " + newExtension);
						if (newExtension != null) {
							this.newFileName = getOutputFileName(false).replace(".flv", newExtension);

						} else {
							newExtension = ".flv";// just to skip the whole file
													// ext extraction
						}
					}

					log.info("HDS: Newfile name: " + this.newFileName);

					Segment s2 = new SegmentImpl(this, folder);
					s2.setTag("HLS");
					s2.setLength(-1);
					log.info("Adding chunk: " + s2);
					chunks.add(s2);
				}
			}
			return true;
		} catch (Exception e) {
			log.info(e.getMessage());
			return false;
		}

	}

	private synchronized void processSegments() {
		int activeCount = getActiveChunkCount();
		log.info("active: " + activeCount);
		if (activeCount < maxCount) {
			int rem = maxCount - activeCount;
			try {
				retryFailedChunks(rem);
			} catch (IOException e) {
				log.info(e.getMessage());
			}
		}
	}

	private void updateStatus() {
		try {
			long now = System.currentTimeMillis();
			if (this.eta == null) {
				this.eta = "---";
			}
			if (converting) {
				progress = this.convertPrg;
			} else if (assembling) {
				long len = length > 0 ? length : downloaded;
				progress = (int) ((totalAssembled * 100) / len);
			} else {
				long downloaded2 = 0;
				int processedSegments = 0;
				int partPrg = 0;
				downloadSpeed = 0;
				for (int i = 0; i < chunks.size(); i++) {
					Segment s = chunks.get(i);
					downloaded2 += s.getDownloaded();
					downloadSpeed += s.getTransferRate();
					if (s.isFinished()) {
						processedSegments++;
					} else if (s.getDownloaded() > 0 && s.getLength() > 0) {
						int prg2 = (int) ((s.getDownloaded() * 100) / s.getLength());
						partPrg += prg2;
					}
				}
				this.downloaded = downloaded2;
				if (chunks.size() > 0) {
					progress = (int) ((processedSegments * 100) / chunks.size());
					progress += (partPrg / chunks.size());
					if (segDet == null) {
						segDet = new SegmentDetails();
						if (segDet.getCapacity() < chunks.size()) {
							segDet.extend(chunks.size() - segDet.getCapacity());
						}
						segDet.setChunkCount(chunks.size());
					}
					SegmentInfo info = segDet.getChunkUpdates().get(0);
					info.setDownloaded(progress);
					info.setLength(100);
					info.setStart(0);
					long diff = downloaded - lastDownloaded;
					long timeSpend = now - prevTime;
					if (timeSpend > 0) {
						float rate = ((float) diff / timeSpend) * 1000;
						if (rate > downloadSpeed) {
							downloadSpeed = rate;
						}

						int prgDiff = progress - lastProgress;
						if (prgDiff > 0) {
							long eta = (timeSpend * (100 - progress) / 1000 * prgDiff);// prgDiff
							lastProgress = progress;
							this.eta = FormatUtilities.hms((int) eta);
						}

						prevTime = now;
						lastDownloaded = downloaded;
					}
				}
			}

			listener.downloadUpdated(id);
		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}

	@Override
	public void stop() {
		stopFlag = true;
		saveState();
		for (int i = 0; i < chunks.size(); i++) {
			chunks.get(i).stop();
		}
		if (this.ffmpeg != null) {
			this.ffmpeg.stop();
		}

		listener.downloadStopped(id);
		listener = null;
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
			log.info("Restore success");
			this.lastDownloaded = downloaded;
			this.lastProgress = this.progress;
			this.prevTime = System.currentTimeMillis();
			if (allFinished()) {
				assembleAsync();
			} else {
				log.info("Starting");
				start();
			}
		} catch (Exception e) {
			log.info(e.getMessage());
			this.errorCode = DManagerConstants.RESUME_FAILED;
			listener.downloadFailed(this.id);
			return;
		}
	}

	@Override
	public int getType() {
		return DManagerConstants.HLS;
	}

	@Override
	public boolean isFileNameChanged() {
		return newFileName != null;
	}

	@Override
	public String getNewFile() {
		return newFileName;
	}

	@Override
	public HttpMetadata getMetadata() {
		return this.metadata;
	}

	private void saveState() {
		if (chunks.size() < 0)
			return;
		StringBuffer sb = new StringBuffer();
		sb.append(this.length + "\n");
		sb.append(downloaded + "\n");
		sb.append(((long) this.totalDuration) + "\n");
		sb.append(urlList.size() + "\n");
		for (int i = 0; i < urlList.size(); i++) {
			String url = urlList.get(i);
			sb.append(url + "\n");
		}
		sb.append(chunks.size() + "\n");
		for (int i = 0; i < chunks.size(); i++) {
			Segment seg = chunks.get(i);
			sb.append(seg.getId() + "\n");
			if (seg.isFinished()) {
				sb.append(seg.getLength() + "\n");
				sb.append(seg.getStartOffset() + "\n");
				sb.append(seg.getDownloaded() + "\n");
			} else {
				sb.append("-1\n");
				sb.append(seg.getStartOffset() + "\n");
				sb.append(seg.getDownloaded() + "\n");
			}
		}
		if (!StringUtils.isNullOrEmptyOrBlank(lastModified)) {
			sb.append(this.lastModified + "\n");
		}
		try {
			File tmp = new File(folder, System.currentTimeMillis() + ".tmp");
			File out = new File(folder, "state.txt");
			FileOutputStream fs = new FileOutputStream(tmp);
			fs.write(sb.toString().getBytes());
			fs.close();
			out.delete();
			tmp.renameTo(out);
		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}

	private boolean restoreState() {
		BufferedReader br = null;
		chunks = new LinkedList<Segment>();
		File file = new File(folder, "state.txt");
		if (!file.exists()) {
			file = getBackupFile(folder);
			if (file == null) {
				return false;
			}
		}
		try {
			br = new BufferedReader(new FileReader(file));
			this.length = Long.parseLong(br.readLine());
			this.downloaded = Long.parseLong(br.readLine());
			this.totalDuration = Long.parseLong(br.readLine());
			int urlCount = Integer.parseInt(br.readLine());
			for (int i = 0; i < urlCount; i++) {
				String url = br.readLine();
				urlList.add(url);
			}
			int chunkCount = Integer.parseInt(br.readLine());
			for (int i = 0; i < chunkCount; i++) {
				String cid = br.readLine();
				long len = Long.parseLong(br.readLine());
				long off = Long.parseLong(br.readLine());
				long dwn = Long.parseLong(br.readLine());
				Segment seg = new SegmentImpl(folder, cid, off, len, dwn);
				seg.setTag("HLS");
				log.info("id: " + seg.getId() + "\nlength: " + seg.getLength() + "\noffset: " + seg.getStartOffset()
						+ "\ndownload: " + seg.getDownloaded());
				chunks.add(seg);
			}
			this.lastModified = br.readLine();
			return true;
		} catch (Exception e) {
			log.info("Failed to load saved state");
			log.info(e.getMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}
		return false;
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

	private String findExtension(String urlStr) {
		String newExtension = null;
		String fileName = DManagerUtils.getFileName(urlStr);
		if (!StringUtils.isNullOrEmptyOrBlank(fileName)) {
			String ext = DManagerUtils.getExtension(fileName);
			if ((!StringUtils.isNullOrEmptyOrBlank(ext)) && ext.length() > 1) {
				if (!ext.toLowerCase().contains("ts")) {
					newExtension = ext;
					if (newExtension.contains("m4s")) {
						log.info("HLS extension: MP4");
						newExtension = ".mp4";
					}
				}
			}
		}
		return newExtension;
	}

	private void assemble() throws IOException {
		InputStream in = null;
		OutputStream out = null;
		totalAssembled = 0L;
		assembling = true;
		assembleFinished = false;

		File outFile = new File(outputFormat == 0 ? getOutputFolder() : folder, getOutputFileName(true));

		try {
			if (stopFlag)
				return;
			log.info("assembling... ");
			out = new FileOutputStream(outFile);
			out.write(flv_sig);
			for (Segment s : chunks) {
				File inFile = new File(folder, s.getId());
				in = new FileInputStream(inFile);
				long streamPos = 0, streamLen = inFile.length();
				while (streamPos < streamLen) {
					if (stopFlag) {
						return;
					}

					long boxsize = readInt32(in);
					streamPos += 4;
					String box_type = readStringBytes(in, 4);
					streamPos += 4;
					if (boxsize == 1) {
						boxsize = readInt64(in) - 16;
						streamPos += 8;
					} else {
						boxsize -= 8;
					}
					if (box_type.equals("mdat")) {
						long boxsz = boxsize;
						while (boxsz > 0) {
							if (stopFlag)
								return;
							int c = (int) (boxsz > b.length ? b.length : boxsz);
							int x = in.read(b, 0, c);
							if (x == -1)
								throw new IOException("Unexpected EOF");
							out.write(b, 0, x);
							boxsz -= x;
							totalAssembled += x;
							long now = System.currentTimeMillis();
							if (now - lastUpdated > 1000) {
								updateStatus();
								lastUpdated = now;
							}
						}
					} else {
						in.skip(boxsize);
					}

					streamPos += boxsize;
				}
				in.close();
			}

			log.info("Output format: " + outputFormat);
			assembleFinished = true;
		} catch (Exception e) {
			log.info(e.getMessage());
		} finally {
			try {
				out.close();
			} catch (Exception e2) {
			}
			try {
				in.close();
			} catch (Exception e2) {
			}
		}
	}

	byte[] b = new byte[8192];

	/*
	 * private void copyBytes(InputStream src, OutputStream dest, long len) throws
	 * IOException { while (len > 0) { if (stopFlag) return; int c = (int) (len >
	 * b.length ? b.length : len); int x = src.read(b, 0, c); if (x == -1) throw new
	 * IOException("Unexpected EOF"); dest.write(b, 0, x); len -= x; } }
	 */

	@Override
	public void progress(int progress) {
		this.convertPrg = progress;
		long now = System.currentTimeMillis();
		if (now - lastUpdated > 1000) {
			updateStatus();
			lastUpdated = now;
		}
	}

	private long readInt32(InputStream s) throws IOException {
		byte[] bytesData = new byte[4];
		if (s.read(bytesData, 0, bytesData.length) != bytesData.length) {
			throw new IOException("Invalid F4F box");
		}
		long iValLo = (long) ((bytesData[3] & 0xff) + ((long) (bytesData[2] & 0xff) * 256));
		long iValHi = (long) ((bytesData[1] & 0xff) + ((long) (bytesData[0] & 0xff) * 256));
		long iVal = iValLo + (iValHi * 65536);
		return iVal;
	}

	private long readInt64(InputStream s) throws IOException {
		long iValHi = readInt32(s);
		long iValLo = readInt32(s);

		long iVal = iValLo + (iValHi * 4294967296L);
		return iVal;
	}

	private String readStringBytes(InputStream s, long len) throws IOException {
		StringBuilder resultValue = new StringBuilder(4);
		for (int i = 0; i < len; i++) {
			resultValue.append((char) s.read());
		}
		return resultValue.toString();
	}

	// public static void main(String[] args) {
	// try {
	// Thread.sleep(5000);
	// } catch (InterruptedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// HlsDownloader d2 = new HlsDownloader(UUID.randomUUID().toString(),
	// "C:\\Users\\sd00109548\\Desktop\\temp");
	// d2.metadata = new HlsMetadata();
	// d2.metadata.setUrl("http://localhost:8080/test.m3u8");
	// d2.start();
	// }

}
