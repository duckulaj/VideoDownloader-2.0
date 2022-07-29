package com.hawkins.dmanager.downloaders;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.hawkins.dmanager.DManagerApp;
import com.hawkins.dmanager.DManagerConstants;
import com.hawkins.dmanager.DownloadEntry;
import com.hawkins.dmanager.DownloadListener;
import com.hawkins.dmanager.downloaders.http.HttpChannel;
import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;
import com.hawkins.dmanager.util.FormatUtilities;
import com.hawkins.dmanager.util.HttpDateParser;
import com.hawkins.dmanager.util.StringUtils;
import com.hawkins.messages.JobprogressMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Downloader implements SegmentListener {
	

	protected volatile boolean stopFlag;
	protected boolean isJavaClientRequired;
	protected long length;
	protected String folder;
	protected String id;
	protected boolean finished;
	protected int maxCount = 8; // 8 is used as default, at runtime derived from Config.getInstance
	public DownloadListener listener;
	protected long downloaded;
	protected long lastDownloaded;
	protected long prevTime;
	protected int progress;
	protected long lastUpdated;
	protected long lastSaved;
	protected boolean assembling;
	protected float downloadSpeed;
	protected String eta;
	protected SegmentDetails segDet;
	protected int errorCode;
	protected int outputFormat;
	protected boolean converting;
	protected int convertPrg;
	protected String lastModified;

	protected String OriginalFileName;
	
	public LinkedList<Segment> chunks;

	public abstract void start();

	public abstract void stop();

	public abstract void resume();

	public abstract int getType();

	public long getSize() {
		return length;
	}

	public int getProgress() {
		return progress;
	}

	public long getDownloaded() {
		return downloaded;
	}

	public abstract boolean isFileNameChanged();

	public abstract String getNewFile();

	public abstract HttpMetadata getMetadata();

	public String getId() {
		return this.id;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public boolean isAssembling() {
		return assembling;
	}

	public float getDownloadSpeed() {
		return downloadSpeed;
	}

	public String getEta() {
		return eta;
	}

	public SegmentDetails getSegmentDetails() {
		return segDet;
	}

	public void setOuputMediaFormat(int format) {
		this.outputFormat = format;
	}

	protected synchronized int retryFailedChunks(int rem) throws IOException {
		if (stopFlag)
			return 0;
		int count = 0;
		int totalInactive = findTotalInactiveChunk();
		if (log.isDebugEnabled()) {
			log.debug("Total inactive chunks: {}", totalInactive);
		}

		if (totalInactive > rem) {
			totalInactive = rem;
		}
		if (totalInactive > 0) {
			for (; totalInactive > 0; totalInactive--) {
				Segment c = findInactiveChunk();
				if (c != null) {
					c.download(this);
					count++;
				} else {
					if (log.isDebugEnabled()) {
						log.debug("$$$ debug rem: {}", rem);
					}
				}
			}
		}
		return count;
	}

	protected Segment findInactiveChunk() {
		if (stopFlag)
			return null;
		for (int i = 0; i < chunks.size(); i++) {
			Segment c = chunks.get(i);
			if (c.isFinished() || c.isActive())
				continue;
			return c;
		}
		return null;
	}

	protected int findTotalInactiveChunk() {
		int count = 0;
		for (int i = 0; i < chunks.size(); i++) {
			Segment c = chunks.get(i);
			if (c.isFinished() || c.isActive())
				continue;
			count++;
		}
		return count;
	}

	public int getActiveChunkCount() {
		int count = 0;
		for (int i = 0; i < chunks.size(); i++) {
			if (chunks.get(i).isActive()) {
				count++;
			}
		}
		return count;
	}

	public void registerListener(DownloadListener listener) {
		this.listener = listener;
	}

	public void unregisterListener() {
		this.listener = null;
	}

	protected boolean allFinished() {
		if (!chunks.isEmpty()) {
			for (int i = 0; i < chunks.size(); i++) {
				Segment chunk = chunks.get(i);
				if (!chunk.isFinished()) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	protected Segment getById(String id) {
		for (int i = 0; i < chunks.size(); i++) {
			if (chunks.get(i).getId().equals(id)) {
				return chunks.get(i);
			}
		}
		return null;
	}

	public void cleanup() {
		File dir = new File(folder);
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (log.isDebugEnabled()) {
				log.debug("Delete: {} [{}] {}", files[i], + files[i].length(), files[i].delete());
			}
		}
		if (new File(folder).delete()) {
			if (log.isDebugEnabled()) {
				log.debug("{} deleted", folder);
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("{} not deleted", folder);
			}
		}
	}

	// call this method before calling socket.recv
	// so that thread waits before all length manipulation is
	// done without data corruption
	public synchronized void synchronize() {

	}

	@Override
	public synchronized void chunkFailed(String id, String reason) {
		if (stopFlag)
			return;
		int err = 0;
		for (int i = 0; i < chunks.size(); i++) {
			Segment chunk = chunks.get(i);
			if (chunk.isActive()) {
				return;
			}
			if (chunk.getErrorCode() != 0) {
				err = chunk.getErrorCode();
			}
		}

		if (err == DManagerConstants.ERR_INVALID_RESP) {
			if (downloaded > 0) {
				if (length > 0) {
					if (chunks.size() > 1) {
						this.errorCode = DManagerConstants.ERR_SESSION_FAILED;
					} else {
						this.errorCode = DManagerConstants.ERR_NO_RESUME;
					}
				} else {
					this.errorCode = DManagerConstants.ERR_NO_RESUME;
				}
			} else {
				this.errorCode = DManagerConstants.ERR_INVALID_RESP;
			}
		} else {
			log.info("Setting final error code: {}", err);
			this.errorCode = err;
		}

		if (this.listener != null) {
			this.listener.downloadFailed(this.id);
		}
		log.info("chunk with id {} failed", id);
	}

	protected String getOutputFileName(boolean updated) {
		return listener.getOutputFile(id, updated);
	}

	protected String getOutputFolder() {
		return listener.getOutputFolder(id);
	}

	@Override
	public synchronized boolean promptCredential(String msg, boolean proxy) {
		return DManagerApp.getInstance().promptCredential(id, msg, proxy);
	}

	protected File getBackupFile(String folder) {
		File f = new File(folder);
		File files[] = f.listFiles();
		if (files == null || files.length < 1)
			return null;
		for (File file : files) {
			if (file.getName().endsWith(".bak")) {
				return file;
			}
		}
		return null;
	}

	public void setLastModifiedDate(File outFile) {
		try {
			Date lastModified = HttpDateParser.parseHttpDate(this.lastModified);
			if (lastModified != null) {
				outFile.setLastModified(lastModified.getTime());
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}

	public void getLastModifiedDate(Segment c) {
		if (StringUtils.isNullOrEmpty(lastModified)) {
			try {
				this.lastModified = ((HttpChannel) c.getChannel()).getHeader("last-modified");
			} catch (Exception e) {
				log.info(e.getMessage());
			}
		}
	}
	
	public void sendProgress(String jobName, DownloadEntry de, SimpMessagingTemplate template) {

		JobprogressMessage temp = new JobprogressMessage(jobName);
		
		temp.setProgress(de.getProgress());
		temp.setState(FormatUtilities.getFormattedStatus(de));
		temp.setDownloadSpeed(Math.round(de.getDownloadSpeed()));
		temp.setOriginalFileName(de.getOriginalFileName());

		template.convertAndSend("/topic/status", temp);
	}

	public String getOriginalFileName() {
		return OriginalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		OriginalFileName = originalFileName;
	}

	
}
