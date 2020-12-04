package com.hawkins.m3u;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hawkins.properties.DownloadProperties;
import com.hawkins.utils.Constants;
import com.hawkins.utils.Utils;

public class M3UtoStrm {

	private static final Logger logger = LogManager.getLogger(M3UtoStrm.class.getName());

	private static String[] videoTypes = {Constants.AVI, Constants.MKV, Constants.MP4};
	private static String tvShowRegex = "[S]{1}[0-9]{2} [E]{1}[0-9]{2}";
	private static String seasonRegex = "[S]{1}[0-9]{2}";
	
	private static M3UGroupList groups = M3UGroupList.getInstance();
	private static M3UPlayList playlist = M3UPlayList.getInstance();
	private static DownloadProperties downloadProperties = DownloadProperties.getInstance();
	
	private static String baseDirectory = downloadProperties.getDownloadPath() + File.separator;

	public static void convertM3UtoStream () {

		/*
		 * 1. Get an instance of the group list
		 * 2. For each group decide if it is TV VOD or Film VOD
		 * 3. For films we need to create a folder for each film
		 * 4. In each folder write out the link to a strm file
		 * 5. For TV Shows create a folder
		 * 6. Create a subfolder for each Season
		 * 7. Create a an strm file for each episode within a season
		 */

		playlist.getPlayList().forEach(item -> {
			String groupTitle = item.getGroupTitle();
			M3UGroup thisGroup = M3UParser.getGroupByName(groups, groupTitle);

			if (thisGroup!= null) {

				String streamType = deriveStreamType(item);

				thisGroup.setType(streamType);
				item.setGroupType(streamType);
			}
		});

		List<M3UItem> movies = filterItems(playlist.getPlayList(), ofType(Constants.MOVIE));
		List<M3UItem> tvshows = filterItems(playlist.getPlayList(), ofType(Constants.TVSHOW));
		
		createMovieFolders(movies);
		createTVshowFolders(tvshows);
	}


	public static String deriveStreamType (M3UItem item) {

		String streamType = null;
		String videoExtension = null;
		String stream= item.getUrl();
		String name = item.getName();

		// Get the last three characters from the stream

		if (stream.length() > 3) videoExtension = stream.substring(stream.length() - 3);

		if (Arrays.asList(videoTypes).contains(videoExtension)) {

			// Check to see if we have Season and Episode information in the form of S01 E01

			Pattern pattern = Pattern.compile(tvShowRegex, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(name);
			boolean matchFound = matcher.find();

			if (matchFound) {
				streamType = Constants.TVSHOW;
			} else {
				streamType = Constants.MOVIE;
			}

		} else {
			streamType = Constants.LIVE;
		}

		return streamType;
	}


	public static Predicate<M3UItem> ofType(String type) {
		return p -> p.getGroupType().equals(type);
	}
	
	public static Predicate<M3UItem> ofTypeDefinition(String definition) {
		return p -> p.getName().indexOf(definition) != -1 && p.getGroupType().equalsIgnoreCase(Constants.MOVIE);
	}

	public static List<M3UItem> filterItems (List<M3UItem> items, Predicate<M3UItem> predicate) {

		return items.stream().filter( predicate ).collect(Collectors.<M3UItem>toList());
	}

	public static void createMovieFolders (List<M3UItem> movies) {

		/*
		 * Movies can be FHD, UHD or normal definition.
		 * We want the UHD to be th final version.
		 */
		 
		List<M3UItem> FHDMovies = filterItems(playlist.getPlayList(), ofTypeDefinition(Constants.FHD));
		List<M3UItem> UHDMovies = filterItems(playlist.getPlayList(), ofTypeDefinition(Constants.UHD));
		
		if (logger.isDebugEnabled()) {
			logger.debug("Starting createMovieFolders");
		}
		
		deleteFolder(Constants.FOLDER_MOVIES);
		String movieFolder = createFolder(Constants.FOLDER_MOVIES + File.separator);
		
		if (logger.isDebugEnabled()) {
			logger.debug("Processing {} movies", movies.size());
			logger.debug("Processing {} FHDMovies", FHDMovies.size());
			logger.debug("Processing {} UHDMovies", UHDMovies.size());
		}
		
		movies.forEach(movie -> {
			if (!movie.getGroupTitle().contains(Constants.UHD) && !movie.getGroupTitle().contains(Constants.FHD)) {
				String folder = movie.getName().trim();
				folder = folder.replace("/", " ").trim();
				String url = movie.getUrl();
				
				try {
					
					String newFolder = folder.trim();
					String newFolderPath = createFolder(Constants.FOLDER_MOVIES + File.separator + newFolder);
					File thisFile = new File(newFolderPath + File.separator + folder + ".strm"); 
					writeToFile(thisFile, url);
				} catch (IOException ioe) {
					ioe.getMessage();
				}
			}
		});
		
		FHDMovies.forEach(movie -> {
			String folder = movie.getName().trim();
			folder = folder.replace("FHD", "").trim();
			folder = folder.replace("/", " ").trim();
			String url = movie.getUrl();
			
			try {
				
				String newFolderPath = createFolder(Constants.FOLDER_MOVIES + File.separator + folder);
				File thisFile = new File(newFolderPath + File.separator + folder + ".strm"); 
				writeToFile(thisFile, url);
			} catch (IOException ioe) {
				ioe.getMessage();
			}
		});
		
		UHDMovies.forEach(movie -> {
			String folder = movie.getName().trim();
			folder = folder.replace("UHD", "").trim();
			folder = folder.replace("/", " ").trim();
			String url = movie.getUrl();
			
			try {
				
				String newFolderPath = createFolder(Constants.FOLDER_MOVIES + File.separator + folder);
				File thisFile = new File(newFolderPath + File.separator + folder + ".strm"); 
				writeToFile(thisFile, url);
			} catch (IOException ioe) {
				ioe.getMessage();
			}
		});
		
		
		
	}

	public static void createTVshowFolders (List<M3UItem> tvshows) {

		if (logger.isDebugEnabled()) {
			logger.debug("Starting createMovieFolders");
		}
		
		deleteFolder(Constants.FOLDER_TVSHOWS);
		String tvShowFolder = createFolder(Constants.FOLDER_TVSHOWS) + File.separator;
		
		if (logger.isDebugEnabled()) {
			logger.debug("Processing {} TV Shows", tvshows.size());
		}
		
		tvshows.forEach(tvShow -> {
			String tvShowName = tvShow.getName();
			tvShowName = Utils.replaceCharacterWithSpace(tvShowName);
			Pattern seasonPattern = Pattern.compile(seasonRegex, Pattern.CASE_INSENSITIVE);
			Matcher seasonMatcher = seasonPattern.matcher(tvShowName);
			boolean seasonMatchFound = seasonMatcher.find();

			if (seasonMatchFound) {
				String season = seasonMatcher.group();
				int seasonStartIndex = seasonMatcher.start();
				/*
				 * Create the TV Show folder
				 */
				String folder = tvShowName.substring(0, seasonStartIndex - 1).trim();
				File tvShowSeasonFolder = new File(tvShowFolder + folder);
				if (!tvShowSeasonFolder.exists()) tvShowSeasonFolder.mkdir();
				/*
				 * Create the Season folder
				 */
				File seasonFolder = new File(tvShowSeasonFolder.getAbsolutePath() + File.separator + season);
				if (!seasonFolder.exists()) seasonFolder.mkdir();
				
				File thisFile = new File(seasonFolder.getAbsolutePath() + File.separator + tvShowName + ".strm"); 
				try {
					writeToFile(thisFile, tvShow.getUrl());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		});
	}

	public static void deleteFolder (String folder) {

		Path pathToBeDeleted = new File(folder).toPath();

		try {
			if (pathToBeDeleted.toFile().exists()) {
				Files.walk(pathToBeDeleted)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static String createFolder (String folder) {

				
		File newDirectory = new File(baseDirectory + folder);

		if (newDirectory.exists()) {
			deleteFolder(newDirectory.getAbsolutePath());
		}
		
		newDirectory.mkdir();
		
		if (logger.isDebugEnabled()) {
			logger.debug("Created folder {}", newDirectory.getAbsolutePath());
		}

		return newDirectory.getAbsolutePath();
		
	}

	public static void writeToFile(File thisFile, String content) throws IOException{

		if (logger.isDebugEnabled()) {
			logger.debug("Writing file {}", thisFile.getAbsolutePath());
		}
		
		FileWriter writer = new FileWriter(thisFile);
		writer.write(content);

		writer.close();
	}

}