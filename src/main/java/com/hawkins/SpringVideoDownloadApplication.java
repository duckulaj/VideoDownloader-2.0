package com.hawkins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan(basePackages = {"com.hawkins"})
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SpringVideoDownloadApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringVideoDownloadApplication.class);

	public static void main(String[] args) {
		System.setProperty("http.KeepAlive.remainingData", "0");
		System.setProperty("http.KeepAlive.queuedConnections", "0");
		System.setProperty("sun.net.http.errorstream.enableBuffering", "false");
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");
		
		if (logger.isDebugEnabled()) {
			logger.debug("loading...{}", "");
			logger.debug("Properties :: Java Version {}, OS Version {}", System.getProperty("java.version"), System.getProperty("os.version"));
		}	
		SpringApplication.run(SpringVideoDownloadApplication.class, args);
	}

}
