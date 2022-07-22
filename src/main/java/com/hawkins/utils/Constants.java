package com.hawkins.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {
	
	public Constants() {
		
	}

	public static final String CANCELLED = "CANCELLED";
	public static final String RUNNING = "RUNNING";
	public static final String NEW = "NEW";
	public static final String PAUSED = "PAUSED";
	
	public static final String CONFIGPROPERTIES = "config.properties";
	public static final String DMPROPERTIES = "dm.properties";
	
	public static final String AVI = "avi";
	public static final String MKV = "mkv";
	public static final String MP4 = "mp4";
	
	public static final String LIVE = "live";
	public static final String TVSHOW = "tvshow";
	public static final String MOVIE = "movie";
	
	public static final String DOWNLOAD = "download";
	public static final String GROUPS = "groups";
	public static final String SELECTEDGROUP = "selectedGroup";
	public static final String SEARCHFILTER = "searchFilter";
	public static final String MOVIEDB = "movieDb";
	public static final String FILMS = "films";
	public static final String JOBLIST = "jobList";
	public static final String SEARCHYEAR = "searchYear";
	public static final String SETTINGS = "settings";
	public static final String ROWS = "rows";
	public static final String STATUS = "status";
	
	public static final String FOLDER_MOVIES = "Movies";
	public static final String FOLDER_TVSHOWS = "TVshows";
	
	public static final String UHD = "UHD";
	public static final String FHD = "FHD";
	public static final String SD = "SD";
	public static final String HD = "HD";
	public static final String ADULT = "For Adults";
	
	public static final String[] allowedExtensions = {"mp4","ts"};
	public static final List<String> toBeConverted = new ArrayList<String>(Arrays.asList(".ts"));
	
	// public static final Long maxFileSize = 2147483648L;
	public static final Long maxFileSize = 1073741824L;
	
	
}
