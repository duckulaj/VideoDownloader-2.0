package com.hawkins.dmanager.downloaders.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hawkins.dmanager.DManagerConstants;
import com.hawkins.dmanager.downloaders.AbstractChannel;
import com.hawkins.dmanager.downloaders.Segment;
import com.hawkins.dmanager.downloaders.SegmentDownloader;
import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;
import com.hawkins.dmanager.util.NetUtils;
import com.hawkins.dmanager.util.DManagerUtils;

public class HttpDownloader extends SegmentDownloader {
	
	private static final Logger logger = LogManager.getLogger(HttpDownloader.class.getName());

	private HttpMetadata metadata;
	private String newFileName;
	private boolean isJavaClientRequired;

	public HttpDownloader(String id, String folder, HttpMetadata metadata) {
		super(id, folder);
		this.metadata = metadata;
	}

	@Override
	public AbstractChannel createChannel(Segment segment) {
		
		HttpChannel hc = new HttpChannel(segment, metadata.getUrl(), metadata.getHeaders(), length, isJavaClientRequired); 
		return hc;

	}

	@Override
	public int getType() {
		return DManagerConstants.HTTP;
	}

	@Override
	public boolean isFileNameChanged() {
		logger.info("Checking for filename change {}", (newFileName != null));
		return newFileName != null;
	}

	@Override
	public String getNewFile() {
		return newFileName;
	}

	@Override
	protected void chunkConfirmed(Segment c) {
		HttpChannel hc = (HttpChannel) c.getChannel();
		this.isJavaClientRequired = hc.isJavaClientRequired();
		super.getLastModifiedDate(c);
		if (hc.isRedirected()) {
			metadata.setUrl(hc.getRedirectUrl());
			metadata.save();
			if (outputFormat == 0) {
				newFileName = DManagerUtils.getFileName(metadata.getUrl());
				if (logger.isDebugEnabled()) {
					logger.debug("set new filename: {}", newFileName);
					logger.debug("new file name: {}", newFileName);
				}
			}
		}
		String contentDispositionHeader = hc.getHeader("content-disposition");
		if (contentDispositionHeader != null) {
			if (outputFormat == 0) {
				String name = NetUtils.getNameFromContentDisposition(contentDispositionHeader);
				if (name != null) {
					this.newFileName = name;
					if (logger.isDebugEnabled()) {
						logger.debug("set new filename: {}", newFileName);
					}
				}
			}
		}
		if ((hc.getHeader("content-type") + "").contains("/html")) {
			if (this.newFileName != null) {
				String upperStr = this.newFileName.toUpperCase();
				if (!(upperStr.endsWith(".HTML") || upperStr.endsWith(".HTM"))) {
					outputFormat = 0;
					this.newFileName += ".html";
					if (logger.isDebugEnabled()) {
						logger.debug("set new filename: {}", newFileName);
					}
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("new filename: {}", newFileName);
		}
	}

	@Override
	public HttpMetadata getMetadata() {
		return this.metadata;
	}

}
