package com.hawkins.properties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;

import com.hawkins.utils.Constants;
import com.hawkins.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadProperties implements Runnable {


	private static DownloadProperties thisInstance = null;

	private String channels = null;
	private String fullM3U = null;
	private String downloadPath = null;
	private String filter = null;
	private String movieDbAPI = null;
	private String movieDbURL = null;
	private String searchMovieURL = null;
	private String resetM3UFileSchedule = null;
	private String createStreamsSchedule = null;
	private String epgFileName = null;
	private String fileWatcherLocation=null;
	private long fileWatcherPollingDuration = 5000L; //default to 5 seconds

	public DownloadProperties() {

		Properties props = Utils.readProperties(Constants.CONFIGPROPERTIES);

		this.setChannels(props.getProperty("channels"));
		
		
		if (SystemUtils.IS_OS_WINDOWS) {
			this.setDownloadPath(System.getProperty("user.home"));
			this.setFullM3U(this.getDownloadPath() + File.separator + "allChannels.m3u");
		} else if (SystemUtils.IS_OS_LINUX) {
			this.setFullM3U(props.getProperty("fullM3U"));
			this.setDownloadPath(props.getProperty("downloadPath"));
		}
		
		this.setFilter(props.getProperty("filter"));
		this.setMovieDbAPI(props.getProperty("moviedb.apikey"));
		this.setMovieDbURL(props.getProperty("moviedb.searchURL"));
		this.setSearchMovieURL(props.getProperty("moviedb.searchMovieURL"));
		this.setEpgFileName(props.getProperty("epg.filename"));
		this.setFileWatcherLocation(props.getProperty("fileWatcher.location"));
		this.setFileWatcherPollingDuration(Long.parseLong(props.getProperty("filewatcher.pollingDuration")));
		
	}

	public static synchronized DownloadProperties getInstance()
	{
		log.debug("Requesting M3UPlayList instance");

		if (DownloadProperties.thisInstance == null)
		{
			DownloadProperties.thisInstance = new DownloadProperties();
		}

		return DownloadProperties.thisInstance;
	}

	public DownloadProperties updateSettings(List<String> newProperties) {

		try {
			Path sourceFile = Paths.get("config.properties");
			Path targetFile = Paths.get("config.properties.bu");

			Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

		} catch (IOException ex) {
			if (log.isDebugEnabled()) {
				log.debug("I/O Error when copying file");
			}
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("Exception copying file");
			}
		}

		Utils.saveProperties((ArrayList<String>) newProperties);
		return new DownloadProperties();

	}

	@Override
	public void run() {
		throw new UnsupportedOperationException();
	}

	public String getChannels() {
		return channels;
	}

	public void setChannels(String channels) {
		this.channels = channels;
	}

	public String getFullM3U() {
		return fullM3U;
	}

	public void setFullM3U(String fullM3U) {
		this.fullM3U = fullM3U;
	}

	public String getDownloadPath() {
		return downloadPath;
	}

	public void setDownloadPath(String downloadPath) {
		this.downloadPath = downloadPath;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public String getMovieDbAPI() {
		return movieDbAPI;
	}

	public String getResetM3UFileSchedule() {
		return resetM3UFileSchedule;
	}

	public void setResetM3UFileSchedule(String resetM3UFileSchedule) {
		this.resetM3UFileSchedule = resetM3UFileSchedule;
	}

	public String getCreateStreamsSchedule() {
		return createStreamsSchedule;
	}

	public void setCreateStreamsSchedule(String createStreamsSchedule) {
		this.createStreamsSchedule = createStreamsSchedule;
	}

	public void setMovieDbAPI(String movieDbAPI) {
		this.movieDbAPI = movieDbAPI;
	}

	public String getMovieDbURL() {
		return movieDbURL;
	}

	public void setMovieDbURL(String movieDbURL) {
		this.movieDbURL = movieDbURL;
	}

	public String getSearchMovieURL() {
		return searchMovieURL;
	}

	public void setSearchMovieURL(String searchMovieURL) {
		this.searchMovieURL = searchMovieURL;
	}

	public String getEpgFileName() {
		return epgFileName;
	}

	public void setEpgFileName(String epgFileName) {
		this.epgFileName = epgFileName;
	}

	public String getFileWatcherLocation() {
		return fileWatcherLocation;
	}

	public void setFileWatcherLocation(String fileWatcherLocation) {
		this.fileWatcherLocation = fileWatcherLocation;
	}

	public long getFileWatcherPollingDuration() {
		return fileWatcherPollingDuration;
	}

	public void setFileWatcherPollingDuration(long fileWatcherPollingDuration) {
		this.fileWatcherPollingDuration = fileWatcherPollingDuration;
	}

}
