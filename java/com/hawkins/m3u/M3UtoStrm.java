package com.hawkins.m3u;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.hawkins.properties.DownloadProperties;
import com.hawkins.utils.Constants;
import com.hawkins.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class M3UtoStrm {


	private static String[] videoTypes = {Constants.AVI, Constants.MKV, Constants.MP4};
	private static String tvShowRegex = "[S]{1}[0-9]{2} [E]{1}[0-9]{2}";
	private static String seasonRegex = "[S]{1}[0-9]{2}";
	
	public static void convertM3UtoStream() {

		/*
		 * 1. Get an instance of the group list
		 * 2. For each group decide if it is TV VOD or Film VOD
		 * 3. For films we need to create a folder for each film
		 * 4. In each folder write out the link to a strm file
		 * 5. For TV Shows create a folder
		 * 6. Create a subfolder for each Season
		 * 7. Create a an strm file for each episode within a season
		 */
		
		M3UGroupList groups = M3UGroupList.getInstance();
		groups.resetGroupList();
		
		M3UPlayList playlist = M3UPlayList.getInstance();

		log.info("{} items in playlist", playlist.getPlayList().size());
		playlist.getPlayList().forEach(item -> {
			String groupTitle = item.getGroupTitle();
			M3UGroup thisGroup = M3UParser.getGroupByName(groups, groupTitle);

			if (thisGroup!= null) {

				String streamType = deriveStreamType(item);

				thisGroup.setType(streamType);
				item.setGroupType(streamType);
			}
		});
		
		List<M3UItem> playlistItems = playlist.getPlayList();

		List<M3UItem> movies = filterItems(playlistItems, ofType(Constants.MOVIE));
		log.info("{} Movies", movies.size());
		
		List<M3UItem> tvshows = filterItems(playlistItems, ofType(Constants.TVSHOW));
		log.info("{} TV Shows", tvshows.size());
		
		List<M3UItem> HDMovies = filterItems(movies, ofTypeDefinition(Constants.HD));
		log.info("{} HD Movies", HDMovies.size());
		
		List<M3UItem> FHDMovies = filterItems(movies, ofTypeDefinition(Constants.FHD));
		log.info("{} FHD Movies", FHDMovies.size());
		
		List<M3UItem> UHDMovies = filterItems(movies, ofTypeDefinition(Constants.UHD));
		log.info("{} UHD Movies", UHDMovies.size());
		
		movies.removeAll(FHDMovies);
		movies.removeAll(UHDMovies);
		
		String movieFolder = createFolder(Constants.FOLDER_MOVIES) + File.separator;
		log.info("Created {}", movieFolder);
		
		createMovieFolders(movies, Constants.HD);
		log.info("Created HD Movies folders");
		
		createMovieFolders(movies, Constants.SD);
		log.info("Created SD Movies folders");
		
		createMovieFolders(FHDMovies, Constants.FHD);
		log.info("Created FHD Movies folders");
		
		createMovieFolders(UHDMovies, Constants.UHD);
		log.info("Created UHD Movies folders");
		
		createTVshowFolders(tvshows);
		log.info("Created TV Shows folders");
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

	public static void createMovieFolders (List<M3UItem> movies, String type) {

		if (log.isDebugEnabled()) {
			log.debug("Starting createMovieFolders");
		}
		
		// deleteFolder(Constants.FOLDER_MOVIES);
				
		if (log.isDebugEnabled()) {
			log.debug("Processing {} movies", movies.size());
		}
		
		log.info("Processing {} movies of type {}", movies.size(),type);
		makeMovieFolders(movies, type);
				
	}

	public static void createTVshowFolders (List<M3UItem> tvshows) {

		if (log.isDebugEnabled()) {
			log.debug("Starting createMovieFolders");
		}
		
		deleteFolder(Constants.FOLDER_TVSHOWS);
		String tvShowFolder = createFolder(Constants.FOLDER_TVSHOWS) + File.separator;
		
		if (log.isDebugEnabled()) {
			log.debug("Processing {} TV Shows", tvshows.size());
		}
		
		log.info("Processing {} TV Shows", tvshows.size());
		
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

		String baseDirectory = DownloadProperties.getInstance().getDownloadPath() + File.separator;
		
		File newDirectory = new File(baseDirectory + folder);

		if (newDirectory.exists()) {
			deleteFolder(newDirectory.getAbsolutePath());
		}
		
		newDirectory.mkdir();
		
		if (log.isDebugEnabled()) {
			log.debug("Created folder {}", newDirectory.getAbsolutePath());
		}

		return newDirectory.getAbsolutePath();
		
	}

	public static void writeToFile(File thisFile, String content) throws IOException{

		if (log.isDebugEnabled()) {
			log.debug("Writing file {}", thisFile.getAbsolutePath());
		}
		
		FileWriter writer = new FileWriter(thisFile);
		writer.write(content);

		writer.close();
	}

	public static void makeMovieFolders(List<M3UItem> movies, String type) {
				
		movies.forEach(movie -> {
			
			String groupTitle = movie.getGroupTitle();
			String folder = movie.getName().trim();
			folder = folder.replace("/", " ").trim();
			
			String url = movie.getUrl();
			
			if (type.equals(Constants.FHD)) {
				folder = folder.replace(Constants.FHD, "").trim();
			} else if (type.equals(Constants.UHD)) {
				folder = folder.replace(Constants.UHD, "").trim();
			}
			try {
				if (!groupTitle.contains(Constants.ADULT)) { // Exclude Adult
				
					String newFolder = M3UParser.normaliseName(folder);
					String newFolderPath = createFolder(Constants.FOLDER_MOVIES + File.separator + newFolder);
					File thisFile = new File(newFolderPath + File.separator + folder + ".strm"); 
					writeToFile(thisFile, url);
				
				}
			} catch (IOException ioe) {
				ioe.getMessage();
			}
		
		});
	}
}