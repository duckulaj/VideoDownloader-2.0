package com.hawkins.dmanager.util;

import java.io.File;
import java.io.FilenameFilter;

import com.hawkins.dmanager.Config;
import com.hawkins.dmanager.DManagerApp;
import com.hawkins.dmanager.network.http.JavaHttpClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateChecker {
	

	private static final String APP_UPDAT_URL = "http://xdman.sourceforge.net/update/update_check.php",
			COMPONENTS_UPDATE_URL = "http://xdman.sourceforge.net/components/update_check.php";
	public static final int APP_UPDATE_AVAILABLE = 10, COMP_UPDATE_AVAILABLE = 20, COMP_NOT_INSTALLED = 30,
			NO_UPDATE_AVAILABLE = 40;

	public static int getUpdateStat() {

		int stat = isComponentUpdateAvailable();
		System.out.println("Stat: "+stat);
		if (stat == 0) {
			return COMP_UPDATE_AVAILABLE;
		} else if (stat == -1) {
			return COMP_NOT_INSTALLED;
		} else {
			System.out.println("checking for app update");
			if (isAppUpdateAvailable())
				return APP_UPDATE_AVAILABLE;
			return NO_UPDATE_AVAILABLE;
		}
	}

	private static boolean isAppUpdateAvailable() {
		return isUpdateAvailable(true, DManagerApp.APP_VERSION);
	}

	// return 1 is no update required
	// return 0, -1 if update required
	private static int isComponentUpdateAvailable() {
		String componentVersion = getComponentVersion();
		System.out.println("current component version: "+componentVersion);
		if (componentVersion == null)
			return -1;
		return isUpdateAvailable(false, componentVersion) ? 0 : 1;
	}

	private static String getComponentVersion() {
		File f = new File(Config.getInstance().getDataFolder());
		String[] files = f.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".version");
			}
		});
		if (files.length < 1) {
			return null;
		}
		return files[0].split("\\.")[0];
	}

	private static boolean isUpdateAvailable(boolean app, String version) {
		JavaHttpClient client = null;
		try {
			client = new JavaHttpClient((app ? APP_UPDAT_URL : COMPONENTS_UPDATE_URL) + "?ver=" + version);
			client.connect();
			int resp = client.getStatusCode();
			log.info("manifest download response: " + resp);
			if (resp == 200) {
				return true;
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		} finally {
			try {
				client.dispose();
			} catch (Exception e) {
			}
		}
		return false;
	}
}
