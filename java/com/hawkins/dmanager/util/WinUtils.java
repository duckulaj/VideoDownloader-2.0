package com.hawkins.dmanager.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import com.hawkins.dmanager.win32.NativeMethods;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WinUtils {
	

	public static void open(File f) throws FileNotFoundException {
		if (!f.exists()) {
			throw new FileNotFoundException();
		}
		try {
			ProcessBuilder builder = new ProcessBuilder();
			ArrayList<String> lst = new ArrayList<String>();
			lst.add("rundll32");
			lst.add("url.dll,FileProtocolHandler");
			lst.add(f.getAbsolutePath());
			builder.command(lst);
			builder.start();
		} catch (IOException e) {
			log.info(e.getMessage());
		}
	}

	public static void openFolder(String folder, String file) throws FileNotFoundException {
		try {
			File f = new File(folder, file);
			if (!f.exists()) {
				throw new FileNotFoundException();
			}
			ProcessBuilder builder = new ProcessBuilder();
			ArrayList<String> lst = new ArrayList<String>();
			lst.add("explorer");
			lst.add("/select,");
			lst.add(f.getAbsolutePath());
			builder.command(lst);
			builder.start();
		} catch (IOException e) {
			log.info(e.getMessage());
		}
	}

	public static void keepAwakePing() {
		NativeMethods.getInstance().keepAwakePing();
	}

	public static void addToStartup() {
		String launchCmd = "\"" + System.getProperty("java.home") + "\\bin\\javaw.exe\" -jar \""
				+ DManagerUtils.getJarFile().getAbsolutePath() + "\" -m";
		log.info("Launch CMD: " + launchCmd);
		NativeMethods.getInstance().addToStartup("DManager", launchCmd);
	}

	public static boolean isAlreadyAutoStart() {
		String launchCmd = "\"" + System.getProperty("java.home") + "\\bin\\javaw.exe\" -jar \""
				+ DManagerUtils.getJarFile().getAbsolutePath() + "\" -m";
		log.info("Launch CMD: " + launchCmd);
		return NativeMethods.getInstance().presentInStartup("DManager", launchCmd);
	}

	public static void removeFromStartup() {
		NativeMethods.getInstance().removeFromStartup("DManager");
	}

	public static void browseURL(String url) {
		try {
			ProcessBuilder builder = new ProcessBuilder();
			ArrayList<String> lst = new ArrayList<String>();
			lst.add("rundll32");
			lst.add("url.dll,FileProtocolHandler");
			lst.add(url);
			builder.command(lst);
			builder.start();
		} catch (IOException e) {
			log.info(e.getMessage());
		}
	}

	
}
