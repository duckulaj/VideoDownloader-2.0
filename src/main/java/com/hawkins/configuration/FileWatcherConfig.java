package com.hawkins.configuration;

import java.io.File;
import java.time.Duration;

import javax.annotation.PreDestroy;

import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hawkins.listener.MyFileChangeListener;
import com.hawkins.properties.DownloadProperties;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FileWatcherConfig {



	@Bean
	public FileSystemWatcher fileSystemWatcher() {
		
		DownloadProperties dp = DownloadProperties.getInstance();
		
		FileSystemWatcher fileSystemWatcher = new FileSystemWatcher(true, Duration.ofMillis(5000L), Duration.ofMillis(3000L));
		//fileSystemWatcher.addSourceDirectory(new File("/home/jonathan/Downloads/xteveFiles"));
		fileSystemWatcher.addSourceDirectory(new File(dp.getFileWatcherLocation()));
		fileSystemWatcher.addListener(new MyFileChangeListener());
		fileSystemWatcher.start();
		log.info("Watching {}", dp.getFileWatcherLocation() );
		
		return fileSystemWatcher;
	}

	@PreDestroy
	public void onDestroy() throws Exception {
		fileSystemWatcher().stop();
	}
}
