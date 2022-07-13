package com.hawkins.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.hawkins.epg.EpgReader;
import com.hawkins.m3u.M3UGroup;
import com.hawkins.m3u.M3UGroupList;
import com.hawkins.m3u.M3UParser;
import com.hawkins.m3u.M3UtoStrm;
import com.hawkins.properties.DmProperties;
import com.hawkins.properties.DownloadProperties;
import com.hawkins.utils.Constants;
import com.hawkins.utils.MovieDb;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class m3u2strmController {

		
	DownloadProperties downloadProperties = DownloadProperties.getInstance();
	DmProperties dmProperties = DmProperties.getInstance();
	M3UGroupList grouplist = M3UGroupList.getInstance();
	
	@ModelAttribute
	public void initValues(Model model) {
		
	}

	@GetMapping("/convertToStream") public String convertM3UtoStream(Model model) {
		
		log.info("Starting convertM3UtoStream()");
		
		EpgReader epgReader = new EpgReader();
		epgReader.changeLocalTime();
		
		M3UtoStrm.convertM3UtoStream();
		
		model.addAttribute(Constants.GROUPS, M3UParser.sortGrouplist(grouplist.getGroupList()));
		model.addAttribute(Constants.SELECTEDGROUP, new M3UGroup());
		model.addAttribute(Constants.SEARCHFILTER, new String());
		model.addAttribute(Constants.MOVIEDB, MovieDb.getInstance());
		return Constants.DOWNLOAD;
	}
}
