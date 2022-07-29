package com.hawkins.dmanager.monitoring;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.UUID;

import com.hawkins.dmanager.Config;
import com.hawkins.dmanager.DManagerApp;
import com.hawkins.dmanager.downloaders.metadata.DashMetadata;
import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;
import com.hawkins.dmanager.network.http.HeaderCollection;
import com.hawkins.dmanager.network.http.HttpHeader;
import com.hawkins.dmanager.network.http.JavaHttpClient;
import com.hawkins.dmanager.util.DManagerUtils;
import com.hawkins.dmanager.util.FormatUtilities;
import com.hawkins.dmanager.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MonitoringSession implements Runnable {
	

	

	private Socket sock;
	private InputStream inStream;
	private OutputStream outStream;
	private Request request;
	private Response response;

	public MonitoringSession(Socket socket) {
		this.sock = socket;
		this.request = new Request();
		this.response = new Response();
		System.out.println("New session");
	}

	public void start() {
		Thread t = new Thread(this);
		t.setDaemon(true);
		t.start();
	}

	private void setResponseOk(Response res) {
		res.setCode(200);
		res.setMessage("OK");
		HeaderCollection headers = new HeaderCollection();
		headers.setValue("content-type", "application/json");
		headers.setValue("Cache-Control", "max-age=0, no-cache, must-revalidate");
		res.setHeaders(headers);
	}

	private void onDownload(Request request, Response res) throws UnsupportedEncodingException {
		try {
			log.info(new String(request.getBody()));
			byte[] b = request.getBody();
			ParsedHookData data = ParsedHookData.parse(b);
			if (data.getUrl() != null && data.getUrl().length() > 0) {
				HttpMetadata metadata = new HttpMetadata();
				metadata.setUrl(data.getUrl());
				metadata.setHeaders(data.getRequestHeaders());
				metadata.setSize(data.getContentLength());
				String file = data.getFile();
				DManagerApp.getInstance().addDownload(metadata, file);
			}
		} finally {
			setResponseOk(res);
		}
	}

	private void onVideo(Request request, Response res) throws UnsupportedEncodingException {
		try {
			log.info(new String(request.getBody()));
			if (!Config.getInstance().isShowVideoNotification()) {
				return;
			}
			byte[] b = request.getBody();
			ParsedHookData data = ParsedHookData.parse(b);
			String type = data.getContentType();
			if (type == null) {
				type = "";
			}
			if (type.contains("f4f") || type.contains("m4s") || type.contains("mp2t") || data.getUrl().contains("fcs")
					|| data.getUrl().contains("abst") || data.getUrl().contains("f4x")
					|| data.getUrl().contains(".fbcdn")) {
				return;
			}
			if (!(processDashSegment(data) || processVideoManifest(data))) {
				processNormalVideo(data);
			}
		} finally {
			setResponseOk(res);
		}
	}

	private void onQuit(Request request, Response res) {
		DManagerApp.getInstance().exit();
	}

	private void onCmd(Request request, Response res) {
		byte[] data = request.getBody();
		if (data == null || data.length < 1) {
			// XDMApp.getInstance().showMainWindow();
		} else {
			String[] arr = new String(data).split("\n");
			for (int i = 0; i < arr.length; i++) {
				String str = arr[i];
				int index = str.indexOf(":");
				if (index < 1)
					continue;
				String key = str.substring(0, index).trim();
				String val = str.substring(index + 1).trim();
				if (key.equals("url")) {
					String url = val;
					HttpMetadata metadata = new HttpMetadata();
					metadata.setUrl(url);
					String file = DManagerUtils.getFileName(url);
					DManagerApp.getInstance().addDownload(metadata, file);
				}
			}
		}
		setResponseOk(res);
	}

	private void onSync(Request request, Response res) {
		StringBuffer json = new StringBuffer();
		json.append("{\n\"enabled\": ");
		json.append(Config.getInstance().isBrowserMonitoringEnabled());
		json.append(",\n\"blockedHosts\": [");
		appendArray(Config.getInstance().getBlockedHosts(), json);
		json.append("],");
		json.append("\n\"videoUrls\": [");
		appendArray(Config.getInstance().getVidUrls(), json);
		json.append("],");
		json.append("\n\"fileExts\": [");
		appendArray(Config.getInstance().getFileExts(), json);
		json.append("],");
		json.append("\n\"vidExts\": [");
		appendArray(Config.getInstance().getVidExts(), json);
		json.append("]");
		json.append("\n}");

		byte[] b = json.toString().getBytes();

		res.setCode(200);
		res.setMessage("OK");

		HeaderCollection headers = new HeaderCollection();
		headers.addHeader("Content-Length", b.length + "");
		headers.addHeader("Content-Type", "application/json");
		res.setHeaders(headers);
		res.setBody(b);
	}

	private void appendArray(String[] arr, StringBuffer buf) {
		boolean insertComma = false;
		if (arr != null && arr.length > 0) {
			for (int i = 0; i < arr.length; i++) {
				if (insertComma) {
					buf.append(",");
				} else {
					insertComma = true;
				}
				buf.append("\"" + arr[i] + "\"");
			}
		}
	}
	
//	private void onFF(Request request, Response res) {
//		ByteArrayOutputStream bout=new ByteArrayOutputStream();
//		InputStream inStream = StringResource.class
//				.getResourceAsStream("/addons/" + "xdm_ff_webext.xpi");
//		
//		byte []b=new byte[8192];
//		
//		while(true){
//			int x=inStream.read(b);
//			if(x==-1)break;
//			bout.write(b,0,x);
//		}
//		
//		b=bout.toByteArray();
//
//		res.setCode(200);
//		res.setMessage("OK");
//
//		HeaderCollection headers = new HeaderCollection();
//		headers.addHeader("Content-Length", b.length + "");
//		headers.addHeader("Content-Type", "application/json");
//		res.setHeaders(headers);
//		res.setBody(b);
//	}


	private void processRequest(Request request, Response res) throws IOException {
		String verb = request.getUrl();
		if (verb.equals("/sync")) {
			onSync(request, response);
		} else if (verb.equals("/download")) {
			onDownload(request, response);
		} else if (verb.equals("/video")) {
			onVideo(request, response);
		} else if (verb.equals("/cmd")) {
			onCmd(request, response);
		} else if (verb.equals("/quit")) {
			onQuit(request, response);
		} else {
			throw new IOException("invalid verb");
		}
	}

	private void serviceRequest() {
		try {
			inStream = sock.getInputStream();
			outStream = sock.getOutputStream();
			while (true) {
				this.request.read(inStream);
				this.processRequest(this.request, this.response);
				this.response.write(outStream);
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		}
		cleanup();
	}

	private void cleanup() {
		try {
			inStream.close();
		} catch (Exception e) {
		}

		try {
			outStream.close();
		} catch (Exception e) {
		}

		try {
			sock.close();
		} catch (Exception e) {
		}
	}

	@Override
	public void run() {
		serviceRequest();
	}

	private boolean processDashSegment(ParsedHookData data) {
		try {
			URL url = new URL(data.getUrl());
			String host = url.getHost();
			if (!(host.contains("youtube.com") || host.contains("googlevideo.com"))) {
				log.info("non yt host");
				return false;
			}
			String type = data.getContentType();
			if (type == null) {
				type = "";
			}
			if (!(type.contains("audio/") || type.contains("video/") || type.contains("application/octet"))) {
				log.info("non yt type");
				return false;
			}
			String low_path = data.getUrl().toLowerCase();
			if (low_path.indexOf("videoplayback") >= 0 && low_path.indexOf("itag") >= 0) {
				// found DASH audio/video stream
				if (StringUtils.isNullOrEmptyOrBlank(url.getQuery())) {
					return false;
				}

				int index = data.getUrl().indexOf("?");

				String path = data.getUrl().substring(0, index);
				String query = data.getUrl().substring(index + 1);

				String arr[] = query.split("&");
				StringBuilder yt_url = new StringBuilder();
				yt_url.append(path + "?");
				int itag = 0;
				long clen = 0;
				String id = "";
				String mime = "";

				for (int i = 0; i < arr.length; i++) {
					String str = arr[i];
					index = str.indexOf("=");
					if (index > 0) {
						String key = str.substring(0, index).trim();
						String val = str.substring(index + 1).trim();
						if (key.startsWith("range")) {
							continue;
						}
						if (key.equals("itag")) {
							itag = Integer.parseInt(val);
						}
						if (key.equals("clen")) {
							clen = Integer.parseInt(val);
						}
						if (key.startsWith("mime")) {
							mime = URLDecoder.decode(val,  StandardCharsets.UTF_8.toString());
						}
						if (str.startsWith("id")) {
							id = val;
						}
					}
					yt_url.append(str);
					if (i < arr.length - 1) {
						yt_url.append("&");
					}
				}
				if (itag != 0) {
					if (YtUtil.isNormalVideo(itag)) {
						log.info("Normal vid");
						return false;
					}
				}

				DASH_INFO info = new DASH_INFO();
				info.url = yt_url.toString();
				info.clen = clen;
				info.video = mime.startsWith("video");
				info.itag = itag;
				info.id = id;
				info.mime = mime;
				info.headers = data.getRequestHeaders();

				log.info("processing yt mime: " + mime + " id: " + id + " clen: " + clen + " itag: " + itag);

				if (YtUtil.addToQueue(info)) {
					DASH_INFO di = YtUtil.getDASHPair(info);

					if (di != null) {
						DashMetadata dm = new DashMetadata();
						dm.setUrl(info.video ? info.url : di.url);
						dm.setUrl2(info.video ? di.url : info.url);
						dm.setLen1(info.video ? info.clen : di.clen);
						dm.setLen2(info.video ? di.clen : info.clen);
						dm.setHeaders(info.video ? info.headers : di.headers);
						dm.setHeaders2(info.video ? di.headers : info.headers);
						String file = data.getFile();
						if (StringUtils.isNullOrEmptyOrBlank(file)) {
							file = DManagerUtils.getFileName(data.getUrl());
						}
						log.info("file: " + file + " url1: " + dm.getUrl() + " url2: " + dm.getUrl2() + " len1: "
								+ dm.getLen1() + " len2: " + dm.getLen2());

						String szStr = null;
						if (info.clen > 0 && di.clen > 0) {
							szStr = FormatUtilities.formatSize(info.clen + di.clen);
						}

						String videoContentType = info.video ? info.mime : di.mime;
						String audioContentType = di.video ? di.mime : info.mime;

						String ext = getYtDashFormat(videoContentType, audioContentType);
						file += "." + ext;

						if (info.video) {

						}

						DManagerApp.getInstance().addMedia(dm, file, YtUtil.getInfoFromITAG(info.video ? info.itag : di.itag)
								+ (szStr == null ? "" : " " + szStr));
						return true;
					}
				}
				return true;
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		}
		return false;
	}

	private boolean processVideoManifest(ParsedHookData data) {
		String url = data.getUrl();
		// String file = data.getFile();
		String contentType = data.getContentType();
		if (contentType == null) {
			contentType = "";
		}
		String ext = DManagerUtils.getExtension(DManagerUtils.getFileName(data.getUrl()));
		File manifestfile = null;

		try {
			if (contentType.contains("mpegurl") || ".m3u8".equalsIgnoreCase(ext)) {
				log.info("Downloading m3u8 manifest");
				manifestfile = downloadMenifest(data);
				return M3U8Handler.handle(manifestfile, data);
			}
			if (contentType.contains("f4m") || ".f4m".equalsIgnoreCase(ext)) {
				log.info("Downloading f4m manifest");
				manifestfile = downloadMenifest(data);
				return F4mHandler.handle(manifestfile, data);
			}
			if (url.contains(".facebook.com") && url.toLowerCase().contains("pagelet")) {
				log.info("Downloading fb manifest");
				manifestfile = downloadMenifest(data);
				return FBHandler.handle(manifestfile, data);
			}
			if (url.contains("player.vimeo.com") && contentType.toLowerCase().contains("json")) {
				log.info("Downloading video manifest");
				manifestfile = downloadMenifest(data);
				return VimeoHandler.handle(manifestfile, data);
			}
			if (url.contains("instagram.com/p/")) {
				log.info("Downloading video manifest");
				manifestfile = downloadMenifest(data);
				return InstagramHandler.handle(manifestfile, data);
			}
		} catch (Exception e) {
		} finally {
			if (manifestfile != null) {
				manifestfile.delete();
			}
		}

		return false;
	}

	private void processNormalVideo(ParsedHookData data) {
		String file = data.getFile();
		String type = data.getContentType();
		if (type == null) {
			type = "";
		}
		if (StringUtils.isNullOrEmptyOrBlank(file)) {
			file = DManagerUtils.getFileName(data.getUrl());
		}
		String ext = "";
		if (type.contains("video/mp4")) {
			ext = "mp4";
		} else if (type.contains("video/x-flv")) {
			ext = "flv";
		} else if (type.contains("video/webm")) {
			ext = "mkv";
		} else if (type.contains("matroska") || type.contains("mkv")) {
			ext = "mkv";
		} else if (type.equals("audio/mpeg") || type.contains("audio/mp3")) {
			ext = "mp3";
		} else if (type.contains("audio/aac")) {
			ext = "aac";
		} else if (type.contains("audio/mp4")) {
			ext = "m4a";
		} else {
			return;
		}
		file += "." + ext;

		if (data.getContentLength() < Config.getInstance().getMinVidSize()) {
			log.info("video less than min size");
			return;
		}

		HttpMetadata metadata = new HttpMetadata();
		metadata.setUrl(data.getUrl());
		metadata.setHeaders(data.getRequestHeaders());
		metadata.setSize(data.getContentLength());
		long size = data.getContentLength();
		if (size > 0) {
			if (data.isPartialResponse()) {
				size = -1;
			}
		}
		String sz = (size > 0 ? FormatUtilities.formatSize(size) : "");
		if (ext.length() > 0) {
			sz += " " + ext.toUpperCase();
		}

		DManagerApp.getInstance().addMedia(metadata, file, sz);
	}

	private File downloadMenifest(ParsedHookData data) {
		JavaHttpClient client = null;
		OutputStream out = null;
		try {
			client = new JavaHttpClient(data.getUrl());
			Iterator<HttpHeader> headers = data.getRequestHeaders().getAll();
			while (headers.hasNext()) {
				HttpHeader header = headers.next();
				client.addHeader(header.getName(), header.getValue());
			}
			client.connect();
			int resp = client.getStatusCode();
			log.info("manifest download response: " + resp);
			if (resp == 206 || resp == 200) {
				InputStream in = client.getInputStream();
				File tmpFile = new File(Config.getInstance().getTemporaryFolder(), UUID.randomUUID().toString());
				long len = client.getContentLength();
				out = new FileOutputStream(tmpFile);
				DManagerUtils.copyStream(in, out, len);
				log.info("manifest download successfull");

				return tmpFile;
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		} finally {
			try {
				out.close();
			} catch (Exception e) {
			}
			try {
				client.dispose();
			} catch (Exception e) {
			}
		}
		return null;
	}

	private String getYtDashFormat(String videoContentType, String audioContentType) {
		if (videoContentType == null) {
			videoContentType = "";
		}
		if (audioContentType == null) {
			audioContentType = "";
		}
		if (videoContentType.contains("mp4") && audioContentType.contains("mp4")) {
			return "mp4";
		} else {
			return "mkv";
		}
	}

}
