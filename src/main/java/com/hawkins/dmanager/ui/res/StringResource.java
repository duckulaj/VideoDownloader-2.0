package com.hawkins.dmanager.ui.res;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hawkins.dmanager.Config;
import com.hawkins.dmanager.util.DManagerUtils;

public class StringResource {
	
	private static final Logger logger = LogManager.getLogger(StringResource.class.getName());

	private static Properties strings;

	public static String get(String id) {
		if (strings == null) {
			try {
				boolean en = false;
				String lang = Config.getInstance().getLanguage();
				File langFile = null;
				if ("en".equals(lang)) {
					en = true;
				} else {
					File jarPath = DManagerUtils.getJarFile().getParentFile();
					langFile = new File(jarPath, "lang/" + lang + ".txt");
					if (!langFile.exists()) {
						logger.info("Unable to find language file: " + langFile);
						en = true;
						Config.getInstance().setLanguage("en");
					}
				}
				if (en) {
					loadDefaultLanguage();
				} else {
					loadLanguage(langFile);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return strings.getProperty(id);
	}

	private static void loadDefaultLanguage() throws Exception {
		strings = new Properties();
		InputStream inStream = StringResource.class.getResourceAsStream("/lang/en.txt");
		if (inStream == null) {
			inStream = new FileInputStream("lang/en.txt");
		}
		strings.load(inStream);
	}

	private static void loadLanguage(File f) throws Exception {
		InputStream inStream = new FileInputStream(f);
		strings.load(inStream);
	}
}
