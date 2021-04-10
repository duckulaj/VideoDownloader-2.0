package com.hawkins.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.hawkins.m3u.M3UGroup;
import com.hawkins.m3u.M3UGroupList;
import com.hawkins.m3u.M3UParser;
import com.hawkins.m3u.M3UtoStrm;
import com.hawkins.properties.DmProperties;
import com.hawkins.properties.DownloadProperties;
import com.hawkins.utils.Constants;
import com.hawkins.utils.MovieDb;

@Controller
public class m3u2strmController {

	private static final Logger logger = LogManager.getLogger(m3u2strmController.class.getName());
	
	DownloadProperties downloadProperties = DownloadProperties.getInstance();
	DmProperties dmProperties = DmProperties.getInstance();
	M3UGroupList grouplist = M3UGroupList.getInstance();
	
	@ModelAttribute
	public void initValues(Model model) {
		
	}

	@GetMapping("/convertToStream") public String convertM3UtoStream(Model model) {
		
		logger.info("Starting convertM3UtoStream()");
		
		M3UtoStrm.convertM3UtoStream();
		
		model.addAttribute(Constants.GROUPS, M3UParser.sortGrouplist(grouplist.getGroupList()));
		model.addAttribute(Constants.SELECTEDGROUP, new M3UGroup());
		model.addAttribute(Constants.SEARCHFILTER, new String());
		model.addAttribute(Constants.MOVIEDB, MovieDb.getInstance());
		return Constants.DOWNLOAD;
	}
}
