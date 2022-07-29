package com.hawkins.messages;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hawkins.dmanager.Config;
import com.hawkins.dmanager.DownloadEntry;
import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;
import com.hawkins.jobs.DownloadJob;

public class DownloadListChannel{

	public static List<DownloadJob> getList(List<DownloadJob> jobs) {

		File file = new File(Config.getInstance().getDataFolder(), "downloads.json");

		DownloadEntry[] downloadListJsonArray;

		try {
		Gson gson = new GsonBuilder().create(); 
		downloadListJsonArray = gson.fromJson(Files.newBufferedReader(file.toPath()),DownloadEntry[].class);

		for (DownloadEntry ent : downloadListJsonArray) {

			String id = ent.getId(); 
			if (!ent.isMetaDataFound()) { 
				HttpMetadata metadata = HttpMetadata.load(id); ent.setMetaDataFound(true);
				ent.setMetaData(metadata); 
			} 
			
			DownloadJob thisJob = new DownloadJob();
			thisJob.setFileName(ent.getOriginalFileName());
			thisJob.setFolder(ent.getFolder());
			thisJob.setState(String.valueOf(ent.getState()));
			
			
			jobs.add(thisJob);
			
			}
		
		} catch(Exception e) {
			e.printStackTrace();
		}
		return jobs;
	}

}
