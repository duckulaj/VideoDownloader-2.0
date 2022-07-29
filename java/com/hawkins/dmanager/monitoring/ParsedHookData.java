package com.hawkins.dmanager.monitoring;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.hawkins.dmanager.network.http.HeaderCollection;
import com.hawkins.dmanager.util.DManagerUtils;
import com.hawkins.dmanager.util.NetUtils;


public class ParsedHookData {

	public static ParsedHookData parse(byte[] b) throws UnsupportedEncodingException {
		ParsedHookData data = new ParsedHookData();
		data.requestHeaders = new HeaderCollection();
		data.responseHeaders = new HeaderCollection();
		String strBuf = new String(b,  StandardCharsets.UTF_8);
		String[] arr = strBuf.split("\r\n");
		for (int i = 0; i < arr.length; i++) {
			String str = arr[i];
			if (!str.contains("=")) {
				continue;
			}
			String ln = str;
			int index = ln.indexOf("=");
			String key = ln.substring(0, index).trim().toLowerCase();
			String val = ln.substring(index + 1).trim();
			if (key.equals("url")) {
				data.setUrl(val);
			} else if (key.equals("file")) {
				val = DManagerUtils.getFileName(val);
				data.setFile(val);
			} else if (key.equals("req")) {
				index = val.indexOf(":");
				if (index > 0) {
					String headerName = val.substring(0, index).trim().toLowerCase();
					String headerValue = val.substring(index + 1).trim();
					if (headerName.equals("range") && (!headerValue.startsWith("bytes=0-"))) {
						data.setPartialResponse(true);
					}
					if (!isBlockedHeader(headerName)) {
						data.requestHeaders.addHeader(headerName, headerValue);
					}
					System.out.println(ln);
				}
			} else if (key.equals("res")) {
				index = val.indexOf(":");
				if (index > 0) {
					String headerName = val.substring(0, index).trim().toLowerCase();
					String headerValue = val.substring(index + 1).trim();
					data.responseHeaders.addHeader(headerName, headerValue);
				}
			}
		}
		if (data.responseHeaders.containsHeader("content-length")
				|| data.responseHeaders.containsHeader("content-range")) {
			data.contentLength = NetUtils.getContentLength(data.responseHeaders);
		}
		if (data.responseHeaders.containsHeader("content-type")) {
			data.contentType = NetUtils.getCleanContentType(data.responseHeaders.getValue("content-type"));
		}

		try {
			data.setExt(DManagerUtils.getExtension(DManagerUtils.getFileName(data.getUrl())));
		} catch (Exception e) {
		}
		return data;
	}

	private static boolean isBlockedHeader(String name) {
		for (int i = 0; i < blockedHeaders.length; i++) {
			if (name.startsWith(blockedHeaders[i])) {
				return true;
			}
		}
		return false;
	}

	private static String blockedHeaders[] = { "accept", "if", "authorization", "proxy", "connection", "expect", "TE",
			"upgrade", "range" };

	private String url, file;
	private HeaderCollection requestHeaders;
	private HeaderCollection responseHeaders;
	private long contentLength;
	private String contentType;
	private String ext;
	private boolean partialResponse;

	public final String getUrl() {
		return url;
	}

	public final void setUrl(String url) {
		this.url = url;
	}

	public final String getFile() {
		return file;
	}

	public final void setFile(String file) {
		this.file = file;
	}

	public final HeaderCollection getRequestHeaders() {
		return requestHeaders;
	}

	public final void setRequestHeaders(HeaderCollection requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public final HeaderCollection getResponseHeaders() {
		return responseHeaders;
	}

	public final void setResponseHeaders(HeaderCollection responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	public final long getContentLength() {
		return contentLength;
	}

	public final void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	public final String getContentType() {
		return contentType;
	}

	public final void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
	}

	public boolean isPartialResponse() {
		return partialResponse;
	}

	public void setPartialResponse(boolean partialResponse) {
		this.partialResponse = partialResponse;
	}
}
