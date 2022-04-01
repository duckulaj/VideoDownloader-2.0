package com.hawkins.dmanager.downloaders.metadata;

import com.hawkins.dmanager.DManagerConstants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HdsMetadata extends HttpMetadata {
	

	private int bitRate;

	public HdsMetadata() {
		super();
	}

	@Override
	public int getType() {
		return DManagerConstants.HDS;
	}

	protected HdsMetadata(String id) {
		super(id);
	}

//	@Override
//	public void load(BufferedReader br) throws IOException {
//		url = br.readLine();
//		bitRate = Integer.parseInt(br.readLine());
//		headers = new HeaderCollection();
//		while (true) {
//			String ln = br.readLine();
//			if (ln == null)
//				break;
//			HttpHeader header = HttpHeader.parse(ln);
//			if (header != null) {
//				headers.addHeader(header);
//			}
//		}
//	}

	@Override
	public HttpMetadata derive() {
		log.info("derive hds metadata");
		HdsMetadata md = new HdsMetadata();
		md.setHeaders(this.getHeaders());
		md.setUrl(this.getUrl());
		md.setBitRate(bitRate);
		return md;
	}

//	@Override
//	public void save() {
//		FileWriter fw = null;
//		try {
//			File file = new File(Config.getInstance().getMetadataFolder(), id);
//			fw = new FileWriter(file);
//			fw.write(getType() + "\n");
//			fw.write(url + "\n");
//			fw.write(bitRate + "\n");
//			Iterator<HttpHeader> headerIterator = headers.getAll();
//			while (headerIterator.hasNext()) {
//				HttpHeader header = headerIterator.next();
//				fw.write(header.getName() + ":" + header.getValue() + "\n");
//			}
//			fw.close();
//		} catch (Exception e) {
//			logger.info(e);
//			if (fw != null) {
//				try {
//					fw.close();
//				} catch (Exception ex) {
//				}
//			}
//		}
//	}

	public int getBitRate() {
		return bitRate;
	}

	public void setBitRate(int bitRate) {
		this.bitRate = bitRate;
	}
}
