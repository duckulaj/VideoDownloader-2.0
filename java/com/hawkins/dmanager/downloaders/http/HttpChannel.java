package com.hawkins.dmanager.downloaders.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import com.hawkins.dmanager.DManagerConstants;
import com.hawkins.dmanager.downloaders.AbstractChannel;
import com.hawkins.dmanager.downloaders.Segment;
import com.hawkins.dmanager.network.ProxyResolver;
import com.hawkins.dmanager.network.http.DManagerHttpClient;
import com.hawkins.dmanager.network.http.HeaderCollection;
import com.hawkins.dmanager.network.http.HttpClient;
import com.hawkins.dmanager.network.http.HttpHeader;
import com.hawkins.dmanager.network.http.JavaClientRequiredException;
import com.hawkins.dmanager.network.http.JavaHttpClient;
import com.hawkins.dmanager.network.http.WebProxy;
import com.hawkins.dmanager.util.DManagerUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpChannel extends AbstractChannel {
	

	private String url;
	private HeaderCollection headers;
	private HttpClient hc;
	private InputStream in;
	private boolean javaClientRequired;
	private long firstLength;
	private long totalLength;
	private boolean redirected;
	private String redirectUrl;

	public HttpChannel(Segment chunk, String url, HeaderCollection headers, long totalLength,
			// it may be known from first connection
			// if java client is required
			boolean javaClientRequired) {
		super(chunk);
		this.url = url;
		this.headers = headers;
		this.totalLength = totalLength;
		this.javaClientRequired = javaClientRequired;
	}

	@Override
	protected boolean connectImpl() {
		int sleepInterval = 0;
		boolean isRedirect = false;
		if (stop) {
			closeImpl();
			return false;
		}

		if (!"HLS".equals(chunk.getTag())) {
			if (chunk.getLength() < 0 && chunk.getDownloaded() > 0) {
				errorCode = DManagerConstants.ERR_NO_RESUME;
				closeImpl();
				log.info("server does not support resuming");
				return false;
			}
			try {
				chunk.reopenStream();
			} catch (IOException e) {
				log.info(e.getMessage());
				closeImpl();
				errorCode = DManagerConstants.ERR_NO_RESUME;
				return false;
			}
		} else {
			try {
				chunk.reopenStream();
				chunk.resetStream();
				chunk.setDownloaded(0);
			} catch (IOException e) {
				log.info("Stream rest failed");
				log.info(e.getMessage());
			}
		}
		while (!stop) {
			isRedirect = false;
			try {
				log.debug("Connecting to: " + url + " " + chunk.getTag());
				WebProxy wp = ProxyResolver.resolve(url);
				if (wp != null) {
					javaClientRequired = true;
				}

				if (javaClientRequired) {
					hc = new JavaHttpClient(url);
				} else {
					// this.socketDataRemaining = -1;
					hc = new DManagerHttpClient(url);
				}

				if (headers != null) {
					Iterator<HttpHeader> headerIt = headers.getAll();
					while (headerIt.hasNext()) {
						HttpHeader header = headerIt.next();
						hc.setHeader(header.getName(), header.getValue());
					}
				}

				long length = chunk.getLength();

				// hc.setHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0;
				// rv:51.0) Gecko/20100101 Firefox/51.0");

				long startOff = chunk.getStartOffset() + chunk.getDownloaded();

				long endOff = startOff + length - chunk.getDownloaded();

				long expectedLength = endOff - startOff;

				if (length > 0 && expectedLength > 0) {
					log.debug(chunk + " requesting:- " + "Range:" + "bytes=" + startOff + "-" + (endOff - 1));
					hc.setHeader("Range", "bytes=" + startOff + "-" + (endOff - 1));
				} else {
					hc.setHeader("Range", "bytes=0-");
				}

				hc.connect();

				if (stop) {
					closeImpl();
					return false;
				}

				int code = hc.getStatusCode();

				log.debug(chunk + ": " + code);

				if (code >= 300 && code < 400) {
					closeImpl();
					if (totalLength > 0) {
						errorCode = DManagerConstants.ERR_INVALID_RESP;
						log.info(chunk + " Redirecting twice");
						return false;
					} else {
						url = hc.getResponseHeader("location");
						log.debug(chunk + " location: " + url);
						if (!url.startsWith("http")) {
							if (!url.startsWith("/")) {
								url = "/" + url;
							}
							url = "http://" + hc.getHost() + url;
						}
						url = url.replace(" ", "%20");
						isRedirect = true;
						redirected = true;
						redirectUrl = url;
						throw new Exception("Redirecting to: " + url);
						// log.info("Redirecting to: " + url);
					}
				}

				if (code != 200 && code != 206 && code != 416 && code != 413 && code != 401 && code != 408
						&& code != 407 && code != 503) {
					errorCode = DManagerConstants.ERR_INVALID_RESP;
					closeImpl();
					return false;
				}

				if (code == 407 || code == 401) {
					if (javaClientRequired) {
						log.info("asking for password");
						// boolean proxy = code == 407;
						/*
						 * if (!chunk.promptCredential(hc.getHost(), proxy)) { errorCode =
						 * XDMConstants.ERR_INVALID_RESP; closeImpl(); return false; }
						 */
						closeImpl();
						return false;
					}
					throw new JavaClientRequiredException();
				}

				if ("T1".equals(chunk.getTag()) || "T2".equals(chunk.getTag())) {
					if ("text/plain".equals(hc.getResponseHeader("content-type"))) {
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						InputStream inStr = hc.getInputStream();
						if (log.isDebugEnabled()) {
							log.debug(inStr.toString());
						}
						long len = hc.getContentLength();
						int read = 0;
						if (log.isDebugEnabled()) {
							log.debug("reading url of length: {}", len);
						}
						while (true) {
							if (len > 0 && read == len)
								break;
							int x = inStr.read();
							if (x == -1) {
								if (len > 0) {
									throw new IOException("Unable to read url: unexpected EOF");
								} else {
									break;
								}
							}
							read++;
							if (log.isDebugEnabled()) {
								log.debug(String.valueOf(x));
							}
							bout.write(x);
						}
						byte[] buf = bout.toByteArray();
						url = new String(buf, StandardCharsets.US_ASCII);
						isRedirect = true;
						throw new Exception("Youtube text redirect to: " + url);
					}
				}

				if (((chunk.getDownloaded() + chunk.getStartOffset()) > 0) && code != 206) {
					closeImpl();
					errorCode = DManagerConstants.ERR_NO_RESUME;
					return false;
				}

				// first length will be used if this is the first thread
				// otherwise its value will be lost
				if ("HLS".equals(chunk.getTag())) {
					firstLength = -1;
				} else {
					firstLength = hc.getContentLength();
				}
				// this.socketDataRemaining = firstLength;
				// we should check content range header instead of this
				if (length > 0) {
					if (firstLength != expectedLength)
					// if (chunk.getStartOffset() + chunk.getDownloaded()
					// + firstLength != totalLength)
					{
						log.info(chunk + " length mismatch: expected: " + expectedLength + " got: " + firstLength);
						errorCode = DManagerConstants.ERR_NO_RESUME;
						closeImpl();
						return false;
					}
				}
				if (hc.getContentLength() > 0 && DManagerUtils.getFreeSpace(null) < hc.getContentLength()) {
					log.info("Disk is full");
					errorCode = DManagerConstants.DISK_FAIURE;
					closeImpl();
					return false;
				}

				in = hc.getInputStream();
				if (log.isDebugEnabled()) {
					log.debug("Connection success");
				}
				return true;

			} catch (JavaClientRequiredException e) {
				if (log.isDebugEnabled()) {
					log.debug("java client required");
				}
				javaClientRequired = true;
				sleepInterval = 0;
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug(chunk.toString());
					log.debug(e.getMessage());
				}
				if (isRedirect) {
					closeImpl();
					continue;
				}
				sleepInterval = 5000;
			}

			closeImpl();

			try {
				Thread.sleep(sleepInterval);
			} catch (Exception e) {
			}
		}

		log.info("return as " + errorCode);

		return false;
	}

	@Override
	protected InputStream getInputStreamImpl() {
		return in;
	}

	@Override
	protected long getLengthImpl() {
		return firstLength;
	}

	@Override
	protected void closeImpl() {
		if (hc != null) {
			hc.dispose();
		}
	}

	public boolean isFinished() {
		if (hc instanceof DManagerHttpClient) {
			return ((DManagerHttpClient) hc).isFinished();
		} else {
			return false;
		}
	}

	public boolean isJavaClientRequired() {
		return this.javaClientRequired;
	}

	public boolean isRedirected() {
		return redirected;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public String getHeader(String name) {
		return hc.getResponseHeader(name);
	}

}
