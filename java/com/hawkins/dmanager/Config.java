package com.hawkins.dmanager;

import java.io.File;
import java.io.FileOutputStream;

import com.hawkins.dmanager.util.DManagerUtils;
import com.hawkins.properties.DmProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Config {
	
	

	private boolean monitoring = true;
	private String metadataFolder;
	private String temporaryFolder;
	private String downloadFolder;
	private String dataFolder;
	private int sortField;
	private boolean sortAsc;
	private int categoryFilter;
	private int stateFilter;
	private String searchText;
	private int maxSegments;
	private int minSegmentSize;
	private int speedLimit; // in kb/sec
	private boolean showDownloadWindow;
	private boolean showDownloadCompleteWindow;
	private int maxDownloads;
	private boolean autoShutdown;
	private int duplicateAction;
	private String[] blockedHosts, vidUrls, fileExts, vidExts;
	private String[] defaultFileTypes, defaultVideoTypes;
	private int networkTimeout, tcpWindowSize;
	private int proxyMode;// 0 no-proxy,1 pac, 2 http, 3 socks
	private String proxyPac, proxyHost, socksHost;
	private int proxyPort, socksPort;
	private String proxyUser, proxyPass;
	private boolean showVideoNotification;
	private int minVidSize;
	private boolean keepAwake, execCmd, execAntivir, autoStart;
	private String customCmd, antivirCmd, antivirExe;
	private boolean firstRun;
	private String language;

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	void save() {
		
		File file = new File(System.getProperty("user.home"), "videoDownloader/.dmanager/dm.properties");
		
		try {
			DmProperties.getInstance().store(new FileOutputStream(file), null);
		} catch (Exception e) {
			log.info(e.getMessage());
		}
		
	}

	void load() {
		
		DmProperties.getInstance();
	}

	private static Config _config;

	private Config() {
		
		File f = new File(System.getProperty("user.home"), "videoDownloader/.dmanager");
		if (!f.exists()) {
			f.mkdirs();
		}
		
		dataFolder = f.getAbsolutePath();
		f = new File(dataFolder, "metadata");
		
		if (f.exists()) f.delete();
		f.mkdir();
		
		this.metadataFolder = f.getAbsolutePath();
		f = new File(dataFolder, "temp");
		
		
		if (f.exists()) f.delete();
		f.mkdir();
		
		this.temporaryFolder = f.getAbsolutePath();
		this.downloadFolder = DManagerUtils.getDownloadsFolder();
		if (!new File(this.downloadFolder).exists()) {
			this.downloadFolder = System.getProperty("user.home") +  "videoDownloader/";
		}

		DmProperties properties = DmProperties.getInstance();
		
		this.monitoring = properties.getMonitoring();
		this.showDownloadWindow = properties.isShowDownloadWindow();
		this.setMaxSegments(properties.getMaxSegments());
		this.setMinSegmentSize(properties.getMinSegmentSize());
		this.maxDownloads = properties.getMaxDownloads();
		this.minVidSize = properties.getMinVidSize();
		this.defaultFileTypes = new String[] { "3GP", "7Z", "AVI", "BZ2", "DEB", "DOC", "DOCX", "EXE", "GZ", "ISO",
				"MSI", "PDF", "PPT", "PPTX", "RAR", "RPM", "XLS", "XLSX", "SIT", "SITX", "TAR", "JAR", "ZIP" };
		this.fileExts = defaultFileTypes;
		this.autoShutdown = properties.isAutoShutdown();
		this.blockedHosts = new String[] { "update.microsoft.com", "windowsupdate.com", "thwawte.com" };
		this.defaultVideoTypes = new String[] { "MP4", "M3U8", "F4M", "WEBM", "OGG", "MP3", "AAC", "FLV", "MKV", "DIVX",
				"MOV", "MPG", "MPEG", "OPUS" };
		this.vidExts = defaultVideoTypes;
		this.vidUrls = new String[] { ".facebook.com|pagelet", "player.vimeo.com/", "instagram.com/p/" };
		this.networkTimeout = properties.getNetworkTimeout();
		this.tcpWindowSize = properties.getTcpWindowSize();
		this.speedLimit = properties.getSpeedLimit();
		this.proxyMode = properties.getProxyMode();
		this.proxyPort = properties.getProxyPort();
		this.socksPort = properties.getSocksPort();
		this.proxyPac = this.proxyHost = this.proxyUser = this.proxyPass = this.socksHost = "";
		this.showVideoNotification = properties.isShowVideoNotification();
		this.showDownloadCompleteWindow = properties.isShowDownloadCompleteWindow();
		this.firstRun = true;
		this.language = properties.getLanguage();
		
	}

	public static Config getInstance() {
		if (_config == null) {
			_config = new Config();
		}
		return _config;
	}

	public final String getMetadataFolder() {
		return metadataFolder;
	}

	public final String getTemporaryFolder() {
		return temporaryFolder;
	}

	public final String getDataFolder() {
		return dataFolder;
	}

	public int getX() {
		return -1;
	}

	public int getY() {
		return -1;
	}

	public int getWidth() {
		return -1;
	}

	public int getHeight() {
		return -1;
	}

	public boolean getSortAsc() {
		return sortAsc;
	}

	public void setSortAsc(boolean sortAsc) {
		this.sortAsc = sortAsc;
	}

	public boolean isBrowserMonitoringEnabled() {
		return monitoring;
	}

	public int getSortField() {
		return sortField;
	}

	public void setSortField(int sortField) {
		this.sortField = sortField;
	}

	public int getCategoryFilter() {
		return categoryFilter;
	}

	public void setCategoryFilter(int categoryFilter) {
		this.categoryFilter = categoryFilter;
	}

	public int getStateFilter() {
		return stateFilter;
	}

	public void setStateFilter(int stateFilter) {
		this.stateFilter = stateFilter;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

	public String getDownloadFolder() {
		return downloadFolder;
	}

	public void setDownloadFolder(String downloadFolder) {
		this.downloadFolder = downloadFolder;
	}

	public int getMaxSegments() {
		return maxSegments;
	}

	public void setMaxSegments(int maxSegments) {
		this.maxSegments = maxSegments;
	}

	public int getMinSegmentSize() {
		return minSegmentSize;
	}

	public void setMinSegmentSize(int minSegmentSize) {
		this.minSegmentSize = minSegmentSize;
	}

	public final int getSpeedLimit() {
		return speedLimit;
	}

	public final void setSpeedLimit(int speedLimit) {
		this.speedLimit = speedLimit;
	}

	final boolean showDownloadWindow() {
		return showDownloadWindow;
	}

	public final void setShowDownloadWindow(boolean show) {
		this.showDownloadWindow = show;
	}

	public final int getMaxDownloads() {
		return maxDownloads;
	}

	public final void setMaxDownloads(int maxDownloads) {
		this.maxDownloads = maxDownloads;
	}

	public final boolean isAutoShutdown() {
		return autoShutdown;
	}

	public final void setAutoShutdown(boolean autoShutdown) {
		this.autoShutdown = autoShutdown;
	}

	public String[] getBlockedHosts() {
		return blockedHosts;
	}

	public void setBlockedHosts(String[] blockedHosts) {
		this.blockedHosts = blockedHosts;
	}

	public String[] getVidUrls() {
		return vidUrls;
	}

	public void setVidUrls(String[] vidUrls) {
		this.vidUrls = vidUrls;
	}

	public String[] getFileExts() {
		return fileExts;
	}

	public void setFileExts(String[] fileExts) {
		this.fileExts = fileExts;
	}

	public String[] getVidExts() {
		return vidExts;
	}

	public void setVidExts(String[] vidExts) {
		this.vidExts = vidExts;
	}

	public final int getDuplicateAction() {
		return duplicateAction;
	}

	public final void setDuplicateAction(int duplicateAction) {
		this.duplicateAction = duplicateAction;
	}

	public final void setShowDownloadCompleteWindow(boolean show) {
		this.showDownloadCompleteWindow = show;
	}

	public final String[] getDefaultFileTypes() {
		return defaultFileTypes;
	}

	public final void setDefaultFileTypes(String[] defaultFileTypes) {
		this.defaultFileTypes = defaultFileTypes;
	}

	public final String[] getDefaultVideoTypes() {
		return defaultVideoTypes;
	}

	public final void setDefaultVideoTypes(String[] defaultVideoTypes) {
		this.defaultVideoTypes = defaultVideoTypes;
	}

	public final int getNetworkTimeout() {
		return networkTimeout;
	}

	public final void setNetworkTimeout(int networkTimeout) {
		this.networkTimeout = networkTimeout;
	}

	public final int getTcpWindowSize() {
		return tcpWindowSize;
	}

	public final void setTcpWindowSize(int tcpWindowSize) {
		this.tcpWindowSize = tcpWindowSize;
	}

	public final int getProxyMode() {
		return proxyMode;
	}

	public final void setProxyMode(int proxyMode) {
		this.proxyMode = proxyMode;
	}

	public final String getProxyUser() {
		return proxyUser;
	}

	public final void setProxyUser(String proxyUser) {
		this.proxyUser = proxyUser;
	}

	public final String getProxyPass() {
		return proxyPass;
	}

	public final void setProxyPass(String proxyPass) {
		this.proxyPass = proxyPass;
	}

	public final String getProxyPac() {
		return proxyPac;
	}

	public final void setProxyPac(String proxyPac) {
		this.proxyPac = proxyPac;
	}

	public final String getProxyHost() {
		return proxyHost;
	}

	public final void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public final int getProxyPort() {
		return proxyPort;
	}

	public final void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public boolean isShowVideoNotification() {
		return showVideoNotification;
	}

	public void setShowVideoNotification(boolean showVideoNotification) {
		this.showVideoNotification = showVideoNotification;
	}

	public int getMinVidSize() {
		return minVidSize;
	}

	public void setMinVidSize(int minVidSize) {
		this.minVidSize = minVidSize;
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

	public boolean isAutoStart() {
		return autoStart;
	}

	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	public String getCustomCmd() {
		return customCmd;
	}

	public void setCustomCmd(String customCmd) {
		this.customCmd = customCmd;
	}

	public String getAntivirCmd() {
		return antivirCmd;
	}

	public void setAntivirCmd(String antivirCmd) {
		this.antivirCmd = antivirCmd;
	}

	public String getAntivirExe() {
		return antivirExe;
	}

	public void setAntivirExe(String antivirExe) {
		this.antivirExe = antivirExe;
	}

	public boolean isFirstRun() {
		return firstRun;
	}

	public boolean isMonitoring() {
		return monitoring;
	}

	public void setMonitoring(boolean monitoring) {
		this.monitoring = monitoring;
	}

	public boolean isShowDownloadWindow() {
		return showDownloadWindow;
	}

	public boolean isShowDownloadCompleteWindow() {
		return showDownloadCompleteWindow;
	}

	public void setMetadataFolder(String metadataFolder) {
		this.metadataFolder = metadataFolder;
	}

	public void setTemporaryFolder(String temporaryFolder) {
		this.temporaryFolder = temporaryFolder;
	}

	public void setDataFolder(String dataFolder) {
		this.dataFolder = dataFolder;
	}

	public void setFirstRun(boolean firstRun) {
		this.firstRun = firstRun;
	}
	
	
}
