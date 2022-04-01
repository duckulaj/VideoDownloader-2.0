package com.hawkins.controller;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.site.SitePreference;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hawkins.dmanager.DManagerApp;
import com.hawkins.jobs.DownloadJob;
import com.hawkins.m3u.M3UDownloadItem;
import com.hawkins.m3u.M3UGroup;
import com.hawkins.m3u.M3UGroupList;
import com.hawkins.m3u.M3UItem;
import com.hawkins.m3u.M3UParser;
import com.hawkins.m3u.M3UPlayList;
import com.hawkins.properties.DmProperties;
import com.hawkins.properties.DownloadProperties;
import com.hawkins.service.DownloadService;
import com.hawkins.utils.Constants;
import com.hawkins.utils.MovieDb;
import com.hawkins.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class DownloadController {

	
	
	@Qualifier("taskExecutor")
	@Autowired
	private ThreadPoolTaskExecutor myExecutor;

	@Autowired
	private SimpMessagingTemplate template;

	private int jobNumber;
	private ArrayList<DownloadJob> myDownloadList = new ArrayList<>(5);

	@Autowired
	private DownloadService myService;
	
	@Autowired
	DownloadController(DownloadService myService) {
		this.myService = myService;
	}

	M3UPlayList playlist = new M3UPlayList();
	M3UGroupList grouplist = new M3UGroupList();
	DownloadProperties downloadProperties = DownloadProperties.getInstance();
	DmProperties dmProperties = DmProperties.getInstance();
	
	


	@ModelAttribute
	public void initValues(Model model) {
		
		dmProperties = DmProperties.getInstance();;
		downloadProperties = DownloadProperties.getInstance();
		playlist = M3UPlayList.getInstance();
		grouplist = M3UGroupList.getInstance();

	}

	@GetMapping("/")
	// @RequestMapping(path = "/", method = RequestMethod.GET)
	public String initial(Model model, Device device, SitePreference sitePreference) {

		if (log.isDebugEnabled()) {
			log.debug("initial :: Device is {}", device.getDevicePlatform());
			log.debug("initial :: Mobile is {}", device.isMobile());
			log.debug("initial :: Normal is {}", device.isNormal());
			log.debug("initial :: Tablet is {}", device.isTablet());
		}
		model.addAttribute(Constants.GROUPS, M3UParser.sortGrouplist(grouplist.getGroupList()));
		model.addAttribute(Constants.SELECTEDGROUP, new M3UGroup());
		model.addAttribute(Constants.SEARCHFILTER, new String());
		model.addAttribute(Constants.MOVIEDB, MovieDb.getInstance());
		
		
		
		return Constants.DOWNLOAD;
	}

	@GetMapping("/downloadSubmit")
	public String downloadSubmit(Model model, @ModelAttribute(Constants.SELECTEDGROUP) M3UGroup selectedGroup) {
		
		if (log.isDebugEnabled()) {
			log.debug(selectedGroup.getName());
		}
		
		if (!selectedGroup.getName().isEmpty()) {
			List<M3UItem> sortedPlayList = playlist.filterPlayList(selectedGroup.getName());
			model.addAttribute(Constants.FILMS, sortedPlayList);
		} else {
			model.addAttribute(Constants.FILMS, playlist.getPlayList());
		}

		model.addAttribute(Constants.MOVIEDB, MovieDb.getInstance());
		model.addAttribute(Constants.GROUPS, M3UParser.sortGrouplist(grouplist.getGroupList()));
		return Constants.DOWNLOAD;
	}
	
	@GetMapping(value = "/searchPage")
	public String searchPage(Model model) {
		
		model.addAttribute(Constants.SELECTEDGROUP, new M3UGroup());
		model.addAttribute(Constants.GROUPS, grouplist.getGroupList());
		
		return "search";
	}

	@GetMapping(value = "/searchFilter")
	public String searchFilter(Model model,
			@RequestParam(value = "searchFilter", required = false) String searchFilter) {

		if (log.isDebugEnabled() ) {
			log.debug("searchFilter is {}", searchFilter);
		}
		
		if (!searchFilter.isEmpty()) {
			List<M3UItem> sortedList = M3UParser.sortPlaylist(M3UPlayList.getInstance().searchplayList(searchFilter));
			model.addAttribute(Constants.FILMS, sortedList);
		}
		
		model.addAttribute(Constants.MOVIEDB, MovieDb.getInstance());
		model.addAttribute(Constants.SELECTEDGROUP, new M3UGroup());
		model.addAttribute(Constants.GROUPS, grouplist.getGroupList());
		return Constants.DOWNLOAD;

	}
	
	@GetMapping(value = "/searchPerson")
	public String searchPerson(Model model,
			@RequestParam(value = "searchPerson", required = false) String searchPerson) {

		if (log.isDebugEnabled()) {
			log.debug("searchPerson is {}", searchPerson);
		}
		if (!searchPerson.isEmpty()) {
			model.addAttribute(Constants.FILMS, M3UParser.sortPlaylist(M3UPlayList.getInstance().searchplayListByActor(searchPerson)));
		}
		
		model.addAttribute(Constants.MOVIEDB, MovieDb.getInstance());
		model.addAttribute(Constants.SELECTEDGROUP, new M3UGroup());
		model.addAttribute("searchPerson", searchPerson);
		model.addAttribute(Constants.GROUPS, grouplist.getGroupList());
		return Constants.DOWNLOAD;
		
	}
	
	@GetMapping(value = "/searchYear")
	public String searchYear(Model model,
			@RequestParam(value = "searchYear", required = false) String searchYear) {

		if (log.isDebugEnabled()) {
			log.debug("searchYear is {}", searchYear);
		}
		
		if (!searchYear.isEmpty()) {
			List<M3UItem> sortedList = M3UParser.sortPlaylist(M3UPlayList.getInstance().searchplayListByYear(searchYear));
			model.addAttribute(Constants.FILMS, sortedList);
		}
		
		model.addAttribute(Constants.MOVIEDB, MovieDb.getInstance());
		model.addAttribute(Constants.SELECTEDGROUP, new M3UGroup());
		model.addAttribute(Constants.SEARCHYEAR, searchYear);
		model.addAttribute(Constants.GROUPS, grouplist.getGroupList());
		return Constants.DOWNLOAD;
	}

	
	  @RequestMapping("/download") public String downloadForm(Model
	  model, @ModelAttribute(Constants.SELECTEDGROUP) M3UGroup selectedGroup) {
	  
	  if (!selectedGroup.getName().isEmpty()) { model.addAttribute(Constants.FILMS,
	  playlist.filterPlayList(selectedGroup.getName())); }
	  
	  
	  model.addAttribute(Constants.MOVIEDB, MovieDb.getInstance());
	  model.addAttribute(Constants.GROUPS, grouplist.getGroupList());
	  
	  return Constants.DOWNLOAD; }
	 
	
	@PostMapping(value = "/download", params = { "name" })
	public String download(Model model, @RequestParam String name, HttpServletResponse response,
			HttpServletRequest request) {

		if (log.isDebugEnabled()) {
			log.debug("download {}", name);
		}

		try {
			URL url = Utils.getFinalLocation(Utils.getURLFromName(name));

			URLConnection u = url.openConnection();

			long length = 0L;
			try {
				length = Long.parseLong(u.getHeaderField("Content-Length"));
			} catch (NumberFormatException nfe) {
				log.debug(nfe.getMessage());
			}
			String type = u.getHeaderField("Content-Type");
			String lengthString = Utils.format(length, 2);

			if (log.isDebugEnabled()) {
				log.debug("File of type {} is {}", type, lengthString);
			}

			M3UDownloadItem downloadItem = new M3UDownloadItem();
			downloadItem.setUrl(url);
			downloadItem.setName(
					downloadProperties.getDownloadPath() + name + "." + Utils.getFileExtension(url.toString()));
			downloadItem.setFilmName(name);
			downloadItem.setSearchPhrase("");
			downloadItem.setSize(length);

			jobNumber ++;
			DownloadJob downloadJob = new DownloadJob(downloadItem.getUrl().toString(), "Job-" + jobNumber, downloadItem.getName(), template);
			downloadJob.setFileName(name + "." + Utils.getFileExtension(url.toString()));
			downloadJob.setFolder(downloadProperties.getDownloadPath());
			
			myService.doWork(downloadJob);
			
			model.addAttribute(Constants.MOVIEDB, MovieDb.getInstance());
			model.addAttribute(Constants.FILMS, playlist.getPlayList());
			model.addAttribute(Constants.GROUPS, grouplist.getGroupList());
			model.addAttribute(Constants.SELECTEDGROUP, new M3UGroup());

		} catch (IOException ioe) {
			log.info(ioe.getMessage());
		} catch (Exception e) {
			log.info(e.getMessage());
		}

		return Constants.STATUS;
	}



	@GetMapping(value = "/showStatus")
	public String showStatus(Model model) {
		
		model.addAttribute(Constants.JOBLIST, this.myDownloadList);
		return Constants.STATUS;
	}
	
	@RequestMapping(value = "/status")
	@ResponseBody
	@SubscribeMapping("initial")
	public ArrayList<DownloadJob> fetchStatus() {
		return this.myDownloadList; 
	}
	
	
	@GetMapping(value = "interrupt", params = { "name" }) 
	public String interruptJob(Model model, @RequestParam String name) {
	  
	  DManagerApp.getInstance().pauseDownload(name); 
	  DownloadJob job = Utils.findJobByName(myDownloadList, name);
	  
	  if (job != null) job.stop();
	  
	  return Constants.STATUS; 
	}
	 
	
	/*
	 * @PostMapping(value = "resumeJobs") public String resumeJobs(Model model) {
	 * 
	 * DManagerApp.getInstance().resumeDownload("123", true); return STATUS; }
	 */
	
	
	
	/*
	 * @GetMapping(value = "pause", params = { "name" }) public String
	 * pauseJob(Model model, @RequestParam String name) {
	 * 
	 * DManagerApp.getInstance().pauseDownload(name); DownloadJob job =
	 * Utils.findJobByName(myDownloadList, name);
	 * 
	 * if (job != null) { job.pause(); job.setState(Constants.PAUSED); }
	 * 
	 * return STATUS; }
	 */
	
	@PostMapping("/removejobs")
	public String removeJobs() {
		myDownloadList = (ArrayList<DownloadJob>) Utils.removeJobs(myDownloadList);
		return Constants.STATUS;
	}
	
	@GetMapping("/settings")
	public String settings (Model model) {
		
		model.addAttribute(Constants.SETTINGS, DownloadProperties.getInstance());
		return Constants.SETTINGS;
	}
	
	@PostMapping(value = "saveSettings")
	public String saveSettings(Model model, 
			@RequestParam String channels, 
			@RequestParam String fullM3U, 
			@RequestParam String downloadPath,
			@RequestParam String movieDbURL,
			@RequestParam String movieDbAPI,
			@RequestParam String searchMovieURL,
			HttpServletResponse response,
			HttpServletRequest request) {
		
		ArrayList<String> newProperties = new ArrayList<>(Arrays.asList(channels, fullM3U, downloadPath, movieDbURL, movieDbAPI, searchMovieURL));
		
		model.addAttribute(Constants.SETTINGS, downloadProperties.updateSettings(newProperties));
		
		model.addAttribute(Constants.MOVIEDB, new MovieDb());
		model.addAttribute(Constants.SELECTEDGROUP, new M3UGroup());
		model.addAttribute(Constants.GROUPS, grouplist.getGroupList());
		
		return "/download";
	}
	
	
	@GetMapping("/group") public String groupForm(Model model) {	

		model.addAttribute(Constants.ROWS, grouplist.getGroupList()); 
		return "group"; 
	}
	
	@GetMapping("/viewLog") public String viewLog(Model model) {
		
		model.addAttribute("logFile", com.hawkins.utils.FileUtils.fileTail("SpringVideoDownload.log", 100));
		return "viewLog";
	}

	@ModelAttribute("playlist")
	public M3UPlayList populatePlaylist() {

		playlist = M3UPlayList.getInstance();
		return playlist;
	}

	@ModelAttribute("grouplist")
	public M3UGroupList populateGrouplist() {

		grouplist = M3UGroupList.getInstance();
		return grouplist;
	}

}
