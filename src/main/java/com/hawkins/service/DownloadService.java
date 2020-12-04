package com.hawkins.service;

import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

@Service
public class DownloadService {

	private static final Logger logger = LogManager.getLogger(DownloadService.class.getName());
	private boolean stop = false;
	
	@Async
    public Future<Boolean> doWork(Runnable startDownload) {
        
		if (logger.isDebugEnabled()) {
			logger.debug("Got runnable {}", startDownload);
		}
	
        startDownload.run();
        
        stop = true;
        return new AsyncResult<>(stop);
    }
	
	public boolean isStop() {
        return stop;
    }

    /**
     * @param stop the stop to set
     */
    public void setStop(boolean stop) {
        this.stop = stop;
    }
}
