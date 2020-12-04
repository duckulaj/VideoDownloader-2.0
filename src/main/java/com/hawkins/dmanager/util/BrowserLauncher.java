package com.hawkins.dmanager.util;

import java.io.File;

public class BrowserLauncher {
	public static boolean launchFirefox(String args) {
		int os = DManagerUtils.detectOS();
		if (os == DManagerUtils.WINDOWS) {
			File[] ffPaths = { new File(System.getenv("PROGRAMFILES"), "Mozilla Firefox\\firefox.exe"),
					new File(System.getenv("PROGRAMFILES(X86)"), "Mozilla Firefox\\firefox.exe") };
			for (int i = 0; i < ffPaths.length; i++) {
				System.out.println(ffPaths[i]);
				if (ffPaths[i].exists()) {
					return DManagerUtils.exec("\"" + ffPaths[i] + "\" " + args);
				}
			}
		}
		if (os == DManagerUtils.MAC) {
			File[] ffPaths = { new File("/Applications/Firefox.app") };
			for (int i = 0; i < ffPaths.length; i++) {
				if (ffPaths[i].exists()) {
					return MacUtils.launchApp(ffPaths[i].getAbsolutePath(), args);
				}
			}
		}
		return false;
	}

	public static boolean launchChrome(String args) {
		int os = DManagerUtils.detectOS();
		if (os == DManagerUtils.WINDOWS) {
			File[] ffPaths = { new File(System.getenv("PROGRAMFILES"), "Google\\Chrome\\Application\\chrome.exe"),
					new File(System.getenv("PROGRAMFILES(X86)"), "Google\\Chrome\\Application\\chrome.exe"),
					new File(System.getenv("LOCALAPPDATA"), "Google\\Chrome\\Application\\chrome.exe") };
			for (int i = 0; i < ffPaths.length; i++) {
				if (ffPaths[i].exists()) {
					return DManagerUtils.exec("\"" + ffPaths[i] + "\" " + args);
				}
			}
		}
		if (os == DManagerUtils.MAC) {
			File[] ffPaths = { new File("/Applications/Google Chrome.app") };
			for (int i = 0; i < ffPaths.length; i++) {
				if (ffPaths[i].exists()) {
					return MacUtils.launchApp(ffPaths[i].getAbsolutePath(), args);
				}
			}
		}
		return false;
	}
}
