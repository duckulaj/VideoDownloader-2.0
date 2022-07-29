package com.hawkins.component;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hawkins.epg.EpgReader;
import com.hawkins.m3u.M3UtoStrm;
import com.hawkins.properties.DownloadProperties;
import com.hawkins.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ScheduledTasks {



	private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Scheduled(cron = "0 1 1 * * ?") // 1.01am
	public void resetM3UFile() {

		String dateString = dateFormat.format(new Date());
		if (log.isDebugEnabled()) {
			log.debug("The time is now {}", dateString);
		}

		DownloadProperties downloadProperties = DownloadProperties.getInstance();

		Utils.copyUrlToFile(downloadProperties.getChannels(), downloadProperties.getFullM3U());
		if (log.isDebugEnabled()) {
			log.debug("Reloaded m3u file at {}", dateString);
		}
	}


	@Scheduled(cron = "0 1 5 * * ?") // 5.05am
	// @Scheduled(fixedRateString = "${createStreams.fixedRate.in.milliseconds}")
	public void createStreams() {

		M3UtoStrm.convertM3UtoStream();
		log.info("Scheduled Task createStreams() completed");
	}

	@Scheduled(cron = "0 1 10 * * ?") // 1.10am
	// @Scheduled(fixedRateString = "${createStreams.fixedRate.in.milliseconds}")
	public void reloadEPG() {

		DownloadProperties dp = DownloadProperties.getInstance();
		String epgFile = dp.getEpgFileName();

		EpgReader.changeLocalTime(epgFile);
		log.info("Scheduled Task reloadEPG() completed");
	}

}