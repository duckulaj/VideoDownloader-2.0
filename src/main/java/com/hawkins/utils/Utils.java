package com.hawkins.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hawkins.jobs.DownloadJob;
import com.hawkins.m3u.M3UItem;
import com.hawkins.m3u.M3UPlayList;

public class Utils {
	private static final Logger logger = LogManager.getLogger(Utils.class.getName());
	private static String propertyFile;

	public static Properties readProperties(String propertyType) {

		long start = System.currentTimeMillis();

		String userHome = System.getProperty("user.home");

		if(userHome.charAt(userHome.length()-1)!=File.separatorChar){
			userHome += File.separator;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Utils.readProperties :: Looking for {}videoDownloader/.dmanager/{}", userHome, propertyType);
		}
		
		File configFile = new File(userHome, "videoDownloader/.dmanager/" + propertyType);

		if (!configFile.exists() && logger.isDebugEnabled()) {
			logger.debug("{} does not exist", propertyType);
		}

		Properties props = new Properties();

		try {
			FileReader reader = new FileReader(configFile);
			props.load(reader);
			reader.close();
		} catch (FileNotFoundException fnfe) {
			logger.debug(fnfe.toString());
		} catch (IOException ioe) {
			logger.debug(ioe.toString());
		}

		long end = System.currentTimeMillis();
		
		if (logger.isDebugEnabled()) {
			logger.debug("readProperties executed in {} ms", (end - start));
		}
		return props;
	}

	public static Properties saveProperties(List<String> newProperties) {
		try (OutputStream output = new FileOutputStream(Constants.CONFIGPROPERTIES)) {

			Properties prop = new Properties();

			// set the properties value
			prop.setProperty("channels", newProperties.get(0));
			prop.setProperty("fullM3U", newProperties.get(1));
			prop.setProperty("downloadPath", newProperties.get(2));
			prop.setProperty("moviedb.searchURL", newProperties.get(3));
			prop.setProperty("moviedb.apikey", newProperties.get(4));
			prop.setProperty("moviedb.searchMovieURL", newProperties.get(5));

			// save properties to project root folder
			prop.store(output, null);

			if (logger.isDebugEnabled()) {
				logger.debug(prop);
			}

		} catch (IOException io) {
			if (logger.isDebugEnabled()) {
				logger.debug(io.getMessage());
			}
		}

		return readProperties(propertyFile);
	}


	public static void copyUrlToFile(String url, String fileName) {
		long start = System.currentTimeMillis();

		try (
				FileOutputStream fileOS = new FileOutputStream(fileName);
				ReadableByteChannel readChannel = Channels.newChannel((new URL(url)).openStream());	
			){
			
			fileOS.getChannel().transferFrom(readChannel, 0L, Long.MAX_VALUE);
			
		} catch (IOException ioe) {
			if (logger.isDebugEnabled()) {
				logger.debug(ioe.getMessage());
			}
		} 

		long end = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug("copyUrlToFile executed in {} ms", (end - start));
		}
	}

	public static URL getFinalLocation(String address) throws IOException{

		long start = System.currentTimeMillis();
		String originalURL = address;

		URL url = new URL(address);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		int status = conn.getResponseCode();
		if (status != HttpURLConnection.HTTP_OK) 
		{
			if (status == HttpURLConnection.HTTP_MOVED_TEMP
					|| status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER)
			{
				String newLocation = conn.getHeaderField("Location");
				return getFinalLocation(newLocation);
			}
		}

		if (!originalURL.equalsIgnoreCase(address)) logger.debug("Final URL is different to Original URL");

		if (logger.isDebugEnabled()) {
			logger.debug("getFinalLocation took {} ms", (System.currentTimeMillis() - start));
		}

		return new URL(address);
	}

	public static String format(double bytes, int digits) {
		String[] dictionary = { "bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB" };
		int index = 0;
		for (index = 0; index < dictionary.length; index++) {
			if (bytes < 1024) {
				break;
			}
			bytes = bytes / 1024;
		}
		return String.format("%." + digits + "f", bytes) + " " + dictionary[index];
	}

	public static String getURLFromName(String filmName) {

		String url = null;

		M3UPlayList filmList = M3UPlayList.getInstance();

		ListIterator<M3UItem> iFilters = filmList.getPlayList().listIterator();

		while (iFilters.hasNext()) {
			M3UItem m3uItem = iFilters.next();

			if (m3uItem.getName().equalsIgnoreCase(filmName)) {
				url = m3uItem.getUrl();
				break;
			}

		}

		return url;

	}

	public static DownloadJob findJobByName(List<DownloadJob> jobs, String name) {

		DownloadJob thisJob = null;

		ListIterator<DownloadJob> iJobs = jobs.listIterator();

		while (iJobs.hasNext()) {
			DownloadJob j = iJobs.next();
			if (j.getJobName().equalsIgnoreCase(name)) {
				thisJob = j;
				break;
			}
		}

		return thisJob;

	}

	public static List<DownloadJob> removeJobs(List<DownloadJob> jobs) {

		ArrayList<DownloadJob> runningJobs = new ArrayList<>();

		ListIterator<DownloadJob> iJobs = jobs.listIterator();

		while (iJobs.hasNext()) {
			DownloadJob j = iJobs.next();
			if (!j.getState().equalsIgnoreCase(Constants.CANCELLED)) {
				runningJobs.add(j);
			} else {
				try {
					FileUtils.forceDelete(new File(j.getDestination()));
				} catch (IOException e) {
					if (logger.isDebugEnabled()) {
					logger.debug(e.getMessage());
					}
				}
			}

		}

		return runningJobs;
	}


	public static String getFileExtension(String url) {

		return url.substring(url.length() - 3);

	}

	public static boolean containsIgnoreCase(String str, String subString) {
		return str.toLowerCase().contains(subString.toLowerCase());
	}

	public long getRandomLong() {
		long leftLimit = 1L;
		long rightLimit = 10L;

		return leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
	
	}

	public static JsonObject searchplayListByActor(String filter) {

		MovieDb movieDb = MovieDb.getInstance();
		String searchPersonURL = movieDb.getPersonURL();
		String api = movieDb.getApi();
		JsonObject obj = new JsonObject();

		try {

			Map<String, String> parameters = new HashMap<>();
			parameters.put("api_key", api);
			parameters.put("query", filter);

			URL url = new URL(searchPersonURL + "?" + getParamsString(parameters));
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");
			
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = (JsonObject)jsonParser.parse(
				      new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
			
			obj = jsonObject;

		} catch (Exception e) {
			logger.info(e.getMessage());
		}

		return obj;
	}

	public static JsonObject searchplayListByYear(String filter) {

		MovieDb movieDb = MovieDb.getInstance();
		String discoverURL = movieDb.getDiscoverURL();
		String api = movieDb.getApi();
		JsonObject obj = new JsonObject();

		try {

			Map<String, String> parameters = new HashMap<>();
			parameters.put("api_key", api);
			parameters.put("language", "en-GB");
			parameters.put("region", "GB");
			parameters.put("release_date.gte", filter + "-01-01");
			parameters.put("release_date.lte", filter + "-12-31");
			
			URL url = new URL(discoverURL + "?" + getParamsString(parameters));
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");
			
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = (JsonObject)jsonParser.parse(
				      new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
			
			obj = jsonObject;

		} catch (Exception e) {
			logger.info(e.getMessage());
		}

		return obj;
	}

	private static String getParamsString(Map<String, String> params) 
			throws UnsupportedEncodingException{
		StringBuilder result = new StringBuilder();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			result.append("&");
		}

		String resultString = result.toString();
		return resultString.length() > 0
				? resultString.substring(0, resultString.length() - 1)
						: resultString;
	}
	
	public static String replaceCharacterWithSpace (String stringToReplace) {
		
		
		if (stringToReplace != null) {
			if (stringToReplace.contains("/")) {
				stringToReplace = stringToReplace.replace("/", " ");
			}
		}
		
		return stringToReplace;
	}

}

