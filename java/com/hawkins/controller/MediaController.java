package com.hawkins.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import com.hawkins.utils.FileUtils;

@Controller
@ControllerAdvice
public class MediaController {

	private String gSearchFolder;
	
	@GetMapping("/getVideos")
	public String getVideos(Model model, @RequestParam(value = "searchFolder", required = false) String searchFolder) {
		
		try {
			gSearchFolder = searchFolder;
			List<String> filesFound = FileUtils.getFiles(searchFolder);
			
			if (filesFound != null && !filesFound.isEmpty()) {
				model.addAttribute("filesFound", filesFound);
			} else {
				model.addAttribute("filesFound", null);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		model.addAttribute("searchFolder", searchFolder);
		return "mediaPlayer/index";
	
	}
	
	@GetMapping("/mediaPlayer")
	public String initial(Model model) {

		model.addAttribute("searchFolder", "/");
		return "mediaPlayer/search";

	}
	
	
	
	@ModelAttribute
    public void addAttributes(Model model) {
		model.addAttribute("searchFolder", gSearchFolder);
    }
	


}
