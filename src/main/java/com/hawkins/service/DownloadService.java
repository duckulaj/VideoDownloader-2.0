package com.hawkins.service;

import java.util.concurrent.Future;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DownloadService {

	private boolean stop = false;
	
	@Async
    public Future<Boolean> doWork(Runnable startDownload) {
        
		if (log.isDebugEnabled()) {
			log.debug("Got runnable {}", startDownload);
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
