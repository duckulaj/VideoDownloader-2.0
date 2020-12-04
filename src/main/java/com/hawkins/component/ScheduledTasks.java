package com.hawkins.component;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hawkins.properties.DownloadProperties;
import com.hawkins.utils.Utils;

@Component
public class ScheduledTasks {

	private static final Logger logger = LogManager.getLogger("ScheduledTasks.class");

	private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Scheduled(cron = "0 1 1 * * ?") // 1.01am
	public void resetM3UFile() {
		
		String dateString = dateFormat.format(new Date());
		if (logger.isDebugEnabled()) {
			logger.debug("The time is now {}", dateString);
		}
		
		DownloadProperties downloadProperties = DownloadProperties.getInstance();

		Utils.copyUrlToFile(downloadProperties.getChannels(), downloadProperties.getFullM3U());
		if (logger.isDebugEnabled()) {
			logger.debug("Reloaded m3u file at {}", dateString);
		}
	}
}