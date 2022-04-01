package com.hawkins.properties;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.hawkins.utils.Constants;
import com.hawkins.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DmProperties implements Runnable {
	

	private static DmProperties thisInstance = null;
	
	private Boolean monitoring = null;
	private String downloadFolder = null;
	private int maxDownloads = 0;
	private int maxSegments = 0;
	private int networkTimeout = 0;
	private int tcpWindowSize = 0;
	private int minSegmentSize = 0;
	private int minVidSize = 0;
	private int duplicateAction = 0;
	private int speedLimit = 0;
	private boolean showDownloadWindow = false;
	private boolean showDownloadCompleteWindow = false;
	private String blockedHosts = null;
	private String vidUrls = null;
	private String fileExts = null;
	private String vidExts = null;
	private int proxyMode = 0;
	private String proxyPac = null;
	private String proxyHost = null;
	private int proxyPort = 0;
	private String socksHost= null;
	private int socksPort = 0;
	private String proxyUser = null;
	private String proxyPass = null;
	private boolean autoShutdown = false;
	private boolean keepAwake = false;
	private boolean execCmd = false;
	private boolean execAntivir = false;
	private String version = "0.0.01";
	private boolean autoStart = false;
	private String language = "en";
	private boolean showVideoNotification = false;
	
	private Properties props;
	

	public DmProperties () {
		props = Utils.readProperties(Constants.DMPROPERTIES);
		
		if (log.isDebugEnabled()) {
			log.debug("props.size = {}", props.size());
		}
		
		this.setMonitoring(Boolean.valueOf(props.get("monitoring").toString()));
		this.setDownloadFolder(props.get("downloadFolder").toString());
		this.setMaxDownloads(Integer.valueOf(props.get("maxDownloads").toString()));
		this.setMaxSegments(Integer.valueOf(props.get("maxSegments").toString()));
		this.setNetworkTimeout(Integer.valueOf(props.get("networkTimeout").toString()));
		this.setTcpWindowSize(Integer.valueOf(props.get("tcpWindowSize").toString()));
		this.setMinSegmentSize(Integer.valueOf(props.get("minSegmentSize").toString()));
		this.setMinVidSize(Integer.valueOf(props.get("minVidSize").toString()));
		this.setDuplicateAction(Integer.valueOf(props.get("duplicateAction").toString()));
		this.setSpeedLimit(Integer.valueOf(props.get("speedLimit").toString()));
		this.setShowDownloadWindow(Boolean.valueOf(props.get("showDownloadWindow").toString()));
		this.setShowDownloadCompleteWindow(Boolean.valueOf(props.get("showDownloadCompleteWindow").toString()));
		this.setBlockedHosts(props.get("blockedHosts").toString());
		this.setVidUrls(props.get("vidUrls").toString());
		this.setFileExts(props.get("fileExts").toString());
		this.setVidExts(props.get("vidExts").toString());
		this.setProxyMode(Integer.valueOf(props.get("proxyMode").toString()));
		this.setProxyPac(props.get("proxyPac").toString());
		this.setProxyHost(props.get("proxyHost").toString());
		this.setProxyPort(Integer.valueOf(props.get("proxyPort").toString()));
		this.setSocksHost(props.get("socksHost").toString());
		this.setSocksPort(Integer.valueOf(props.get("socksPort").toString()));
		this.setProxyUser(props.get("proxyUser").toString());
		this.setProxyPass(props.get("proxyPass").toString());
		this.setAutoShutdown(Boolean.valueOf(props.get("autoShutdown").toString()));
		this.setKeepAwake(Boolean.valueOf(props.get("keepAwake").toString()));
		this.setExecCmd(Boolean.valueOf(props.get("execAntivir").toString()));
		this.setExecAntivir(Boolean.valueOf(props.get("execCmd").toString()));
		this.setVersion(props.getProperty("version"));
		this.setAutoStart(Boolean.valueOf(props.get("autoStart").toString()));
		this.setLanguage(props.getProperty("language"));
		this.setShowVideoNotification(Boolean.valueOf(props.get("showVideoNotification").toString()));
		
	}

	public static synchronized DmProperties getInstance()
	{
		log.debug("Requesting M3UPlayList instance");

		if (DmProperties.thisInstance == null)
		{
			DmProperties.thisInstance = new DmProperties();
		}

		return DmProperties.thisInstance;
	}
	
	@Override
	public void run() {
		throw new UnsupportedOperationException();
	}
	
	public void store(FileOutputStream fos, String other) {
		
		try {
			this.props.store(fos, other);
		} catch (IOException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
		}
	}

	public Boolean getMonitoring() {
		return monitoring;
	}

	public void setMonitoring(Boolean monitoring) {
		this.monitoring = monitoring;
	}

	public String getDownloadFolder() {
		return downloadFolder;
	}

	public void setDownloadFolder(String downloadFolder) {
		this.downloadFolder = downloadFolder;
	}

	public int getMaxDownloads() {
		return maxDownloads;
	}

	public void setMaxDownloads(int maxDownloads) {
		this.maxDownloads = maxDownloads;
	}

	public int getMaxSegments() {
		return maxSegments;
	}

	public void setMaxSegments(int maxSegments) {
		this.maxSegments = maxSegments;
	}

	public int getNetworkTimeout() {
		return networkTimeout;
	}

	public void setNetworkTimeout(int networkTimeout) {
		this.networkTimeout = networkTimeout;
	}

	public int getTcpWindowSize() {
		return tcpWindowSize;
	}

	public void setTcpWindowSize(int tcpWindowSize) {
		this.tcpWindowSize = tcpWindowSize;
	}

	public int getMinSegmentSize() {
		return minSegmentSize;
	}

	public void setMinSegmentSize(int minSegmentSize) {
		this.minSegmentSize = minSegmentSize;
	}

	public int getMinVidSize() {
		return minVidSize;
	}

	public void setMinVidSize(int minVidSize) {
		this.minVidSize = minVidSize;
	}

	public int getDuplicateAction() {
		return duplicateAction;
	}

	public void setDuplicateAction(int duplicateAction) {
		this.duplicateAction = duplicateAction;
	}

	public int getSpeedLimit() {
		return speedLimit;
	}

	public void setSpeedLimit(int speedLimit) {
		this.speedLimit = speedLimit;
	}

	public boolean isShowDownloadWindow() {
		return showDownloadWindow;
	}

	public void setShowDownloadWindow(boolean showDownloadWindow) {
		this.showDownloadWindow = showDownloadWindow;
	}

	public boolean isShowDownloadCompleteWindow() {
		return showDownloadCompleteWindow;
	}

	public void setShowDownloadCompleteWindow(boolean showDownloadCompleteWindow) {
		this.showDownloadCompleteWindow = showDownloadCompleteWindow;
	}

	public String getBlockedHosts() {
		return blockedHosts;
	}

	public void setBlockedHosts(String blockedHosts) {
		this.blockedHosts = blockedHosts;
	}

	public String getVidUrls() {
		return vidUrls;
	}

	public void setVidUrls(String vidUrls) {
		this.vidUrls = vidUrls;
	}

	public String getFileExts() {
		return fileExts;
	}

	public void setFileExts(String fileExts) {
		this.fileExts = fileExts;
	}

	public String getVidExts() {
		return vidExts;
	}

	public void setVidExts(String vidExts) {
		this.vidExts = vidExts;
	}

	public int getProxyMode() {
		return proxyMode;
	}

	public void setProxyMode(int proxyMode) {
		this.proxyMode = proxyMode;
	}

	public String getProxyPac() {
		return proxyPac;
	}

	public void setProxyPac(String proxyPac) {
		this.proxyPac = proxyPac;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getSocksHost() {
		return socksHost;
	}

	public void setSocksHost(String socksHost) {
		this.socksHost = socksHost;
	}

	public int getSocksPort() {
		return socksPort;
	}

	public void setSocksPort(int socksPort) {
		this.socksPort = socksPort;
	}

	public String getProxyUser() {
		return proxyUser;
	}

	public void setProxyUser(String proxyUser) {
		this.proxyUser = proxyUser;
	}

	public String getProxyPass() {
		return proxyPass;
	}

	public void setProxyPass(String proxyPass) {
		this.proxyPass = proxyPass;
	}

	public boolean isAutoShutdown() {
		return autoShutdown;
	}

	public void setAutoShutdown(boolean autoShutdown) {
		this.autoShutdown = autoShutdown;
	}

	public boolean isKeepAwake() {
		return keepAwake;
	}

	public void setKeepAwake(boolean keepAwake) {
		this.keepAwake = keepAwake;
	}

	public boolean isExecCmd() {
		return execCmd;
	}

	public void setExecCmd(boolean execCmd) {
		this.execCmd = execCmd;
	}

	public boolean isExecAntivir() {
		return execAntivir;
	}

	public void setExecAntivir(boolean execAntivir) {
		this.execAntivir = execAntivir;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isAutoStart() {
		return autoStart;
	}

	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public boolean isShowVideoNotification() {
		return showVideoNotification;
	}

	public void setShowVideoNotification(boolean showVideoNotification) {
		this.showVideoNotification = showVideoNotification;
	}

}