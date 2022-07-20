package com.hawkins.listener;

import java.util.Set;

import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.filewatch.FileChangeListener;

import com.hawkins.epg.EpgReader;
import com.hawkins.properties.DownloadProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyFileChangeListener implements FileChangeListener {

	@Override
	public void onChange(Set<ChangedFiles> changeSet) {

		DownloadProperties dp = DownloadProperties.getInstance();
		String epgFile = dp.getEpgFileName();

		for(ChangedFiles cfiles:changeSet) {
			for(ChangedFile cfile:cfiles.getFiles()) {
				log.info("Evaluating {}", cfile.getFile().getName());
				if (cfile.getFile().toString().equals(epgFile)) {

					log.info("Sending {} for changeLocalTime evaluation", cfile.getFile().getName());

					EpgReader.changeLocalTime(epgFile);
					
					break;
				}

			}
		}

	}
}