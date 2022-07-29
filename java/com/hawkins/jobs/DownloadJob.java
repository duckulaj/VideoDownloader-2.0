package com.hawkins.jobs;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.hawkins.dmanager.DManagerApp;
import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;
import com.hawkins.utils.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadJob implements DetailedJob {

	
	private SimpMessagingTemplate template;

	private String state = Constants.NEW;
	private AtomicInteger progress = new AtomicInteger();
	private String jobName = "";
	private String url = "";
	private String destination = "";
	private String folder = "";
	private String fileName = "";

	private final AtomicBoolean running = new AtomicBoolean(false);

	public DownloadJob() {

	}

	public DownloadJob(String url, String jobName, String destination, SimpMessagingTemplate template) {
		this.jobName = jobName;
		this.template = template;
		this.url = url;
		this.destination = destination;
	}

	@Override
	public void run () {

		state = Constants.RUNNING;
		running.set(true);

		HttpMetadata hmd = new HttpMetadata();
		hmd.setUrl(url);
		hmd.setYdlUrl(url);
		hmd.setSize(getFileLength(url));
				
		DManagerApp.getInstance().createDownload(this.fileName, this.folder, hmd, true, "", 0, 0, this.template);
		
	}
	
	public void stop() {
		DManagerApp.getInstance().pauseDownload(this.fileName);
		state =  Constants.CANCELLED;
		running.set(false);
		
	}
	
	public void pause() {
		DManagerApp.getInstance().pauseDownload(this.fileName);
		state =  Constants.PAUSED;
		running.set(false);
	}

	@Override
	public int getProgress() {
		return progress.get();
	}

	public String getState() {
		return state;
	}

	public String getJobName() {
		return jobName;
	}
	
	public String getDestination() {
		return destination;
	}
	
	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public void setState(String state) {
		this.state = state;
	}
	
	

	private long getFileLength(String url) {
		
		
		long fileLength = 0L;
		try {
			URL thisUrl = new URL(url);

			URLConnection u = thisUrl.openConnection();

			fileLength = Long.parseLong(u.getHeaderField("Content-Length"));
		} catch (NumberFormatException | IOException nfe) {
			if (log.isDebugEnabled()) {
				log.debug(nfe.getMessage());
			}
		}
		
		return fileLength;
	}
}
