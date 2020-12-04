package com.hawkins.messages;

public class JobprogressMessage {
    private String jobName;
    private String state;
    private int progress;
    private String url;
    private int downloadSpeed;
    private String originalFileName;

    public JobprogressMessage(String jobName)
    {
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }

    public String getState() {
        return state;
    }

    public int getProgress() {
        return progress;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getDownloadSpeed() {
		return downloadSpeed;
	}

	public void setDownloadSpeed(int downloadSpeed) {
		this.downloadSpeed = downloadSpeed;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}
    
	
    
}
