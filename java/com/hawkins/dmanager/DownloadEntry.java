package com.hawkins.dmanager;

import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;
import com.hawkins.dmanager.util.FormatUtilities;


public class DownloadEntry {
	private String id, file, folder;
	private int state, category;
	private long size, downloaded;
	private long date;
	private int progress;
	private String dateStr;
	private String queueId;
	private boolean startedByUser;
	private int outputFormatIndex;// 0 orginal
	private float downloadSpeed;
	private String eta;
	private String originalFileName;
	private boolean metaDataFound = false;
	private HttpMetadata metaData;
	
	public DownloadEntry() {
	}

	public String getId() {
		return id;
	}

	public String getDateStr() {
		return dateStr;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getDownloaded() {
		return downloaded;
	}

	public void setDownloaded(long downloaded) {
		this.downloaded = downloaded;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
		this.dateStr = FormatUtilities.formatDate(date);
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public final String getQueueId() {
		return queueId;
	}

	public final void setQueueId(String queueId) {
		this.queueId = queueId;
	}

	public final void setDateStr(String dateStr) {
		this.dateStr = dateStr;
	}

	public final boolean isStartedByUser() {
		return startedByUser;
	}

	public final void setStartedByUser(boolean startedByUser) {
		this.startedByUser = startedByUser;
	}

	public final int getOutputFormatIndex() {
		return outputFormatIndex;
	}

	public final void setOutputFormatIndex(int outputFormatIndex) {
		this.outputFormatIndex = outputFormatIndex;
	}

	public float getDownloadSpeed() {
		return downloadSpeed;
	}

	public void setDownloadSpeed(float downloadSpeed) {
		this.downloadSpeed = downloadSpeed;
	}

	public String getEta() {
		return eta;
	}

	public void setEta(String eta) {
		this.eta = eta;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}

	public boolean isMetaDataFound() {
		return metaDataFound;
	}

	public void setMetaDataFound(boolean metaDataFound) {
		this.metaDataFound = metaDataFound;
	}

	public HttpMetadata getMetaData() {
		return metaData;
	}

	public void setMetaData(HttpMetadata metaData) {
		this.metaData = metaData;
	}

	
	
	
}
