package com.hawkins.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.hawkins.m3u.M3UGroup;
import com.hawkins.m3u.M3UGroupList;
import com.hawkins.m3u.M3UtoStrm;
import com.hawkins.utils.Constants;
import com.hawkins.utils.MovieDb;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class m3u2strmController {

	@ModelAttribute
	public void initValues(Model model) {
		
	}

	@GetMapping("/convertToStream") public String convertM3UtoStream(Model model) {
		
		log.info("Starting convertM3UtoStream()");
		
		M3UtoStrm.convertM3UtoStream();
		
		model.addAttribute(Constants.GROUPS, M3UGroupList.getInstance().getGroupList());
		model.addAttribute(Constants.SELECTEDGROUP, new M3UGroup());
		model.addAttribute(Constants.SEARCHFILTER, new String());
		model.addAttribute(Constants.MOVIEDB, MovieDb.getInstance());
		return Constants.DOWNLOAD;
	}
}
