package com.hawkins.dmanager.monitoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.hawkins.dmanager.DManagerApp;
import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;
import com.hawkins.dmanager.util.DManagerUtils;
import com.hawkins.dmanager.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VimeoHandler {
	


	// public static void main(String[] args) {
	// handle(new File("C:\\Users\\subhro\\Desktop\\video.htm.txt"), null);
	// }

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
			String keyword = "\"progressive\"";
			int index = buf.indexOf(keyword);
			if (index < 0) {
				return false;
			}
			index += keyword.length();
			index = buf.indexOf(":", index);
			if (index < 0) {
				return false;
			}
			index++;
			index = buf.indexOf("[", index);
			if (index < 0) {
				return false;
			}
			index++;
			int start = index;
			index = buf.indexOf("]", index);
			if (index < 0) {
				return false;
			}
			String str = buf.substring(start, index);
			index = 0;
			while (index != -1) {
				index = str.indexOf("{", index);
				if (index > -1) {
					index++;
					start = index;
					index = str.indexOf("}", index);
					if (index > -1) {
						String s = str.substring(start, index);
						processString(s, data);
					}
				}
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		}
		return false;
	}

	private static void processString(String str, ParsedHookData data) {
		String quality = "", type = "", url = "";
		String[] arr = str.split(",");
		for (int i = 0; i < arr.length; i++) {
			int index = arr[i].indexOf(":");
			if (index > 0) {
				String key = arr[i].substring(0, index).replace("\"", "");
				String val = arr[i].substring(index + 1).replace("\"", "");
				if (key.equals("url")) {
					url = val;
					log.info(url);
				}
				if (key.equals("quality")) {
					quality = val;
					log.info(quality);
				}
				if (key.equals("mime")) {
					type = val;
					log.info(type);
				}
			}
		}
		String ext = "mp4";
		if (type.contains("video/mp4")) {
			ext = "mp4";
		} else if (type.contains("video/webm")) {
			ext = "webm";
		}
		HttpMetadata metadata = new HttpMetadata();
		metadata.setUrl(url);
		metadata.setHeaders(data.getRequestHeaders());
		String file = data.getFile();
		if (StringUtils.isNullOrEmptyOrBlank(file)) {
			file = DManagerUtils.getFileName(data.getUrl());
		}
		DManagerApp.getInstance().addMedia(metadata, file + "." + ext, ext.toUpperCase() + " " + quality);
	}
}
