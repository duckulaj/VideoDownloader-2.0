package com.hawkins.dmanager.downloaders.metadata;

import com.hawkins.dmanager.DManagerConstants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HlsMetadata extends HttpMetadata {
	

	public HlsMetadata() {
		super();
	}

	@Override
	public int getType() {
		return DManagerConstants.HLS;
	}

	protected HlsMetadata(String id) {
		super(id);
	}

	@Override
	public HttpMetadata derive() {
		log.info("derive hls metadata");
		HlsMetadata md = new HlsMetadata();
		md.setHeaders(this.getHeaders());
		md.setUrl(this.getUrl());
		return md;
	}

	// @Override
	// public void save() {
	// FileWriter fw = null;
	// try {
	// File file = new File(Config.getInstance().getMetadataFolder(), id);
	// fw = new FileWriter(file);
	// fw.write(getType() + "\n");
	// fw.write(url + "\n");
	// Iterator<HttpHeader> headerIterator = headers.getAll();
	// while (headerIterator.hasNext()) {
	// HttpHeader header = headerIterator.next();
	// fw.write(header.getName() + ":" + header.getValue() + "\n");
	// }
	// fw.close();
	// } catch (Exception e) {
	// logger.info(e);
	// if (fw != null) {
	// try {
	// fw.close();
	// } catch (Exception ex) {
	// }
	// }
	// }
	// }
}
