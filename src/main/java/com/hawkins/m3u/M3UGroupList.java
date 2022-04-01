package com.hawkins.m3u;

import java.util.ArrayList;
import java.util.List;

import com.hawkins.properties.DownloadProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class M3UGroupList {
	

	private List<M3UGroup> groups = new ArrayList<>();
	private static M3UGroupList thisInstance = null;
	
	private DownloadProperties downloadproperties = DownloadProperties.getInstance();
	
	public M3UGroupList() {

		// DownloadProperties downloadproperties = DownloadProperties.getInstance();
		
		this.groups =  M3UParser.sortGrouplist(M3UParser.getM3UGroupsFromFile(downloadproperties.getFullM3U()));
	}

	public static synchronized M3UGroupList getInstance()
	{
		log.debug("Requesting M3UGroupList instance");

		if (M3UGroupList.thisInstance == null)
		{
			M3UGroupList.thisInstance = new M3UGroupList();
		}

		return M3UGroupList.thisInstance;
	}
	
	public List<M3UGroup> getGroupList() {
		return this.groups;
	}
	
	public List<M3UGroup> resetGroupList () {
		this.groups = new ArrayList<>();
		this.groups =  M3UParser.sortGrouplist(M3UParser.getM3UGroupsFromFile(downloadproperties.getFullM3U()));
		return this.groups;
	}
}
