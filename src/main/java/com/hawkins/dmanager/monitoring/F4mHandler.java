package com.hawkins.dmanager.monitoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hawkins.dmanager.DManagerApp;
import com.hawkins.dmanager.downloaders.metadata.HdsMetadata;
import com.hawkins.dmanager.downloaders.metadata.manifests.F4MManifest;
import com.hawkins.dmanager.util.StringUtils;
import com.hawkins.dmanager.util.DManagerUtils;

public class F4mHandler {
	
	private static final Logger logger = LogManager.getLogger(F4mHandler.class.getName());

	public static boolean handle(File f4mfile, ParsedHookData data) {
		try {
			StringBuffer buf = new StringBuffer();
			InputStream in = new FileInputStream(f4mfile);
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			while (true) {
				String ln = r.readLine();
				if (ln == null) {
					break;
				}
				buf.append(ln + "\n");
			}
			in.close();
			logger.info("HDS manifest validating...");
			if (buf.indexOf("http://ns.adobe.com/f4m/1.0") < 0) {
				logger.info("No namespace");
				return false;
			}
			if (buf.indexOf("manifest") < 0) {
				logger.info("No manifest keyword");
				return false;
			}
			if (buf.indexOf("drmAdditional") > 0) {
				logger.info("DRM");
				return false;
			}
			if (buf.indexOf("media") == 0 || buf.indexOf("href") > 0 || buf.indexOf(".f4m") > 0) {
				logger.info("Not a valid manifest");
				return false;
			}

			F4MManifest manifest = new F4MManifest(data.getUrl(), f4mfile.getAbsolutePath());
			long[] bitRates = manifest.getBitRates();
			for (int i = 0; i < bitRates.length; i++) {
				HdsMetadata metadata = new HdsMetadata();
				metadata.setUrl(data.getUrl());
				metadata.setBitRate((int) bitRates[i]);
				metadata.setHeaders(data.getRequestHeaders());
				String file = data.getFile();
				if (StringUtils.isNullOrEmptyOrBlank(file)) {
					file = DManagerUtils.getFileName(data.getUrl());
				}
				DManagerApp.getInstance().addMedia(metadata, file + ".flv", "FLV " + bitRates[i] + " bps");
			}
			return true;
		} catch (Exception e) {
			logger.info(e);
			return false;
		}
	}
}
