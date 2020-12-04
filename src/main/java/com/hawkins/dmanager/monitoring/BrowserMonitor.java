package com.hawkins.dmanager.monitoring;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hawkins.dmanager.DManagerApp;

public class BrowserMonitor implements Runnable {
	
	private static final Logger logger = LogManager.getLogger(BrowserMonitor.class.getName());

	private static BrowserMonitor _this;
	
	public static BrowserMonitor getInstance() {
		if (_this == null) {
			_this = new BrowserMonitor();
		}
		return _this;
	}

	public void startMonitoring() {
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		ServerSocket serverSock = null;
		try {
			serverSock = new ServerSocket();
			serverSock.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 9614));
			// serverSock.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 9714));
			DManagerApp.instanceStarted();
			while (true) {
				Socket sock = serverSock.accept();
				MonitoringSession session = new MonitoringSession(sock);
				session.start();
			}
		} catch (Exception e) {
			logger.info(e);
			DManagerApp.instanceAlreadyRunning();
		}
		try {
			serverSock.close();
		} catch (Exception e) {
		}
	}
}
