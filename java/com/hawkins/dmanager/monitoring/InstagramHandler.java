package com.hawkins.dmanager.monitoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hawkins.dmanager.DManagerApp;
import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;
import com.hawkins.dmanager.util.DManagerUtils;
import com.hawkins.dmanager.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstagramHandler {
	

	private static Pattern pattern;

	public static boolean handle(File tempFile, ParsedHookData data) {
		try {
			StringBuffer buf = new StringBuffer();
			InputStream in = new FileInputStream(tempFile);
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			while (true) {
				String ln = r.readLine();
				if (ln == null) {
					break;
				}
				buf.append(ln + "\n");
			}
			in.close();
			log.info("Parsing instagram page...");
			if (pattern == null) {
				pattern = Pattern.compile("\"video\\_url\"\\s*:\\s*\"(.*?)\"");
			}
			Matcher matcher = pattern.matcher(buf);
			if (matcher.find()) {
				// int start = matcher.start();
				// int end = matcher.end();
				String url = matcher.group(1);
				log.info("Url: " + url);
				HttpMetadata metadata = new HttpMetadata();
				metadata.setUrl(url);
				metadata.setHeaders(data.getRequestHeaders());
				String file = data.getFile();
				if (StringUtils.isNullOrEmptyOrBlank(file)) {
					file = DManagerUtils.getFileName(data.getUrl());
				}
				String ext = DManagerUtils.getExtension(DManagerUtils.getFileName(url));
				if (ext != null) {
					ext = ext.replace(".", "").toUpperCase();
				} else {
					ext = "";
				}
				DManagerApp.getInstance().addMedia(metadata, file + "." + ext, ext);
			}
			return true;
		} catch (Exception e) {
			log.info(e.getMessage());
			return false;
		}
	}
}
