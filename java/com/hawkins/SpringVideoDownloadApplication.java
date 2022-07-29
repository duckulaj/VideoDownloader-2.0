package com.hawkins;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@ComponentScan(basePackages = {"com.hawkins"})
@SpringBootApplication
@EnableAsync
@EnableScheduling
@Slf4j
public class SpringVideoDownloadApplication {

    public static void main(String[] args) {
		System.setProperty("http.KeepAlive.remainingData", "0");
		System.setProperty("http.KeepAlive.queuedConnections", "0");
		System.setProperty("sun.net.http.errorstream.enableBuffering", "false");
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");
		
		if (log.isDebugEnabled()) {
			log.debug("loading...{}", "");
			log.debug("Properties :: Java Version {}, OS Version {}", System.getProperty("java.version"), System.getProperty("os.version"));
		}	
		SpringApplication.run(SpringVideoDownloadApplication.class, args);
	}

}
