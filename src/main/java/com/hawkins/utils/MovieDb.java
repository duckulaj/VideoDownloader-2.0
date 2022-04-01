package com.hawkins.utils;

import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MovieDb implements Runnable {
	
	private String url = "";
	private String api = "";
	private String movieURL = "";
	private String personURL = "";
	private String discoverURL = "";
	

	private static MovieDb thisInstance = null;
	
	public MovieDb() {
		
		Properties props = Utils.readProperties(Constants.CONFIGPROPERTIES);
		
		this.setApi(props.getProperty("moviedb.apikey"));
		this.setUrl(props.getProperty("moviedb.searchURL"));
		this.setMovieURL(props.getProperty("moviedb.searchMovieURL"));
		this.setPersonURL(props.getProperty("moviedb.searchPersonURL"));
		this.setDiscoverURL(props.getProperty("moviedb.discoverURL"));
		
	}
	
	public static synchronized MovieDb getInstance()
	{
		log.debug("Requesting M3UPlayList instance");

		if (MovieDb.thisInstance == null)
		{
			MovieDb.thisInstance = new MovieDb();
		}

		return MovieDb.thisInstance;
	}
	
	@Override
	public void run() {
		throw new UnsupportedOperationException();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String mdURL) {
		this.url = mdURL;
	}

	public String getApi() {
		return api;
	}

	public void setApi(String mdAPI) {
		this.api = mdAPI;
	}

	public String getMovieURL() {
		return movieURL;
	}

	public void setMovieURL(String movieURL) {
		this.movieURL = movieURL;
	}

	public String getPersonURL() {
		return personURL;
	}

	public void setPersonURL(String personURL) {
		this.personURL = personURL;
	}

	public String getDiscoverURL() {
		return discoverURL;
	}

	public void setDiscoverURL(String discoverURL) {
		this.discoverURL = discoverURL;
	}
	
	

	
	
	
	

}
