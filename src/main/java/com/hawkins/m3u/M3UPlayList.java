package com.hawkins.m3u;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hawkins.properties.DownloadProperties;
import com.hawkins.utils.Utils;

public class M3UPlayList implements Runnable {

	private static final Logger logger = LogManager.getLogger(M3UPlayList.class.getName());
	private List<M3UItem> items = new ArrayList<>();
	private static M3UPlayList thisInstance = null;

	public M3UPlayList() {

		DownloadProperties downloadproperties = DownloadProperties.getInstance();
		
		Utils.copyUrlToFile(downloadproperties.getChannels(), downloadproperties.getFullM3U());

		this.items =  M3UParser.getAllM3UListFromFile(downloadproperties.getFullM3U());
	}

	public static synchronized M3UPlayList getInstance()
	{
		if (logger.isDebugEnabled()) {
			logger.debug("Requesting M3UPlayList instance");
		}

		if (M3UPlayList.thisInstance == null)
		{
			M3UPlayList.thisInstance = new M3UPlayList();
		}

		return M3UPlayList.thisInstance;
	}

	public List<M3UItem> filterPlayList(String group) {

		List<M3UItem> sortedItems = new ArrayList<>();

		Iterator<M3UItem> it = this.items.iterator();

		while (it.hasNext()) {
			M3UItem thisItem = it.next();
			if (thisItem.getGroupTitle().equalsIgnoreCase(group) || thisItem.getName().contains(group)) {
				sortedItems.add(thisItem);
			}
		}

		return M3UParser.sortPlaylist(sortedItems);
	}

	public List<M3UItem> searchplayList(String filter) {

		List<M3UItem> foundItems = new ArrayList<>();

		Iterator<M3UItem> it = this.items.iterator();

		while (it.hasNext()) {
			M3UItem thisItem = it.next();
			if (Utils.containsIgnoreCase(thisItem.getName(), filter)) {
				foundItems.add(thisItem);
			}
		}

		return foundItems;
	}

	public List<M3UItem> searchplayListByActor(String filter) {

		JsonObject obj = Utils.searchplayListByActor(filter);
		JsonArray actors = (JsonArray) obj.get("results"); 
		
		if (logger.isDebugEnabled()) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			logger.debug(gson.toJson(actors));
		}

		Iterator<JsonElement> actorsIt = actors.iterator();

		List<String> movies = new ArrayList<>();
		while (actorsIt.hasNext()) {
			JsonObject actor = actorsIt.next().getAsJsonObject();

			JsonArray knownfor = (JsonArray) actor.get("known_for");
			
			if (logger.isDebugEnabled()) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				logger.debug(gson.toJson(knownfor));
			}

			Iterator<JsonElement> knownforIt = knownfor.iterator();

			while (knownforIt.hasNext()) {
				JsonObject movie = knownforIt.next().getAsJsonObject();

				String mediatype = movie.get("media_type").getAsString();

				if (mediatype.equalsIgnoreCase("tv")) {
					movies.add(movie.get("name").getAsString());
				} else if (mediatype.equalsIgnoreCase("movie")) {
					movies.add(movie.get("title").getAsString());
				}

			}


		}

		List<M3UItem> foundItems = new ArrayList<>();

		Iterator<M3UItem> it = this.items.iterator();

		while (it.hasNext()) {
			M3UItem thisItem = it.next();
			if (movies.contains(thisItem.getName())) {
				foundItems.add(thisItem);
			}
		}

		return foundItems;
	}

	public List<M3UItem> searchplayListByYear(String filter) {

		JsonObject obj = Utils.searchplayListByYear(filter);
		JsonArray moviesForYear = (JsonArray) obj.get("results"); 
				
		if (logger.isDebugEnabled()) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			logger.debug(gson.toJson(moviesForYear));
		}

		Iterator<JsonElement> moviesIt = moviesForYear.iterator();

		List<String> movies = new ArrayList<>();
		while (moviesIt.hasNext()) {
			JsonObject movie = moviesIt.next().getAsJsonObject();

			movies.add(movie.get("title").getAsString());
		}

		List<M3UItem> foundItems = new ArrayList<>();

		Iterator<M3UItem> it = this.items.iterator();

		while (it.hasNext()) {
			M3UItem thisItem = it.next();
								
			if (movies.contains(thisItem.getSearch()) || movies.contains(thisItem.getName().replaceAll(" .*", ""))) {
				foundItems.add(thisItem);
			}
		}

		return foundItems;
	}

	public List<M3UItem> getPlayList() {
		return this.items;
	}


	public List<M3UItem> getRows() {
		return this.items;
	}

	@Override
	public void run() {
		throw new UnsupportedOperationException();
	}
	
	
}