package com.hawkins.m3u;

import com.hawkins.utils.Utils;

public class M3UItem {
	private String id = "";
	private String name = "";
	private String logo = "";
	private String title = "";
	private String groupType = "";
	private String url = "";
	private String search = "";
	private String movieDbId = "";
	private boolean downloaded = false;
	
	String[] wordsToExclude= {"[4K]","[HD]"};

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		
		Utils.replaceCharacterWithSpace(name);
		
		this.name = Utils.replaceAndStrip(name, wordsToExclude);
	}

	public String getLogo() {
		return this.logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	public String getGroupTitle() {
		return this.title;
	}

	public void setGroupTitle(String groupTitle) {
		
		Utils.replaceCharacterWithSpace(groupTitle);
		this.title = groupTitle;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String channelURL) {
		this.url = channelURL;
	}
	
	public boolean isDownloaded() {
		return downloaded;
	}

	public void setDownloaded(boolean downloaded) {
		this.downloaded = downloaded;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		
		Utils.replaceCharacterWithSpace(title);
		this.title = title;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public String getMovieDbId() {
		return movieDbId;
	}

	public void setMovieDbId(String movieDbId) {
		this.movieDbId = movieDbId;
	}

	public String getGroupType() {
		return groupType;
	}

	public void setGroupType(String groupType) {
		this.groupType = groupType;
	}
	
}