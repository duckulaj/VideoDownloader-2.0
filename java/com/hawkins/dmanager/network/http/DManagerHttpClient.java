package com.hawkins.dmanager.network.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.hawkins.dmanager.network.FixedRangeInputStream;
import com.hawkins.dmanager.network.HostUnreachableException;
import com.hawkins.dmanager.network.KeepAliveConnectionCache;
import com.hawkins.dmanager.network.NetworkException;
import com.hawkins.dmanager.network.ParsedURL;
import com.hawkins.dmanager.network.SocketFactory;
import com.hawkins.dmanager.util.NetUtils;
import com.hawkins.dmanager.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DManagerHttpClient extends HttpClient {
	

	private ParsedURL _url;
	private Socket socket;
	private String statusLine;
	private long length;
	private FixedRangeInputStream in;
	private boolean keepAliveSupported;
	private boolean closed;

	public DManagerHttpClient(String url) {
		super();
		this._url = ParsedURL.parse(url);
		this.length = -1;
	}

	public boolean isFinished() {
		try {
			return (in.isStreamFinished() && keepAliveSupported);
		} catch (Exception e) {
		}
		return false;
	}

	@Override
	public void dispose() {
		if (closed)
			return;
		closed = true;
		try {
			if (in.isStreamFinished() && keepAliveSupported) {
				releaseSocket();
				return;
			}
		} catch (Exception e) {

		}
		try {
			this.socket.close();
		} catch (Exception e) {

		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return in;
	}

	@Override
	public void connect() throws IOException {
		try {
			int port = _url.getPort();
			String portStr = (port == 80 || port == 443) ? "" : ":" + port;
			requestHeaders.setValue("host", _url.getHost() + portStr);
			Socket sock = KeepAliveConnectionCache.getInstance().getReusableSocket(_url.getHost(), _url.getPort());
			boolean reusing = false;
			if (sock == null) {
				if (log.isDebugEnabled()) {
					log.debug("Creating new socket");
				}
				this.socket = createSocket();
			} else {
				reusing = true;
				if (log.isDebugEnabled()) {
					log.debug("Reusing existing socket");
				}
				this.socket = sock;
			}
			OutputStream sockOut = socket.getOutputStream();
			InputStream sockIn = socket.getInputStream();
			String reqLine = "GET " + _url.getPathAndQuery() + " HTTP/1.1";
			StringBuilder reqBuf = new StringBuilder();
			reqBuf.append(reqLine + "\r\n");
			requestHeaders.appendToBuffer(reqBuf);
			reqBuf.append("\r\n");

			if (log.isDebugEnabled()) {
				log.debug("Sending request:\n {}", reqBuf);
			}
			
			sockOut.write(StringUtils.getBytes(reqBuf));
			sockOut.flush();
			statusLine = NetUtils.readLine(sockIn);

			String[] arr = statusLine.split(" ");
			
			try {
				this.statusCode = Integer.parseInt(arr[1].trim());
			} catch (NumberFormatException nfe) {
				String message = nfe.getMessage();
				
				if (StandardCharsets.US_ASCII.newEncoder().canEncode(message)) {
					if (log.isDebugEnabled()) {
						log.debug(message);
					}
				} else {
					if (log.isDebugEnabled()) {
						log.debug("message contains printable characters encountered");
					}
				}
				if (log.isDebugEnabled()) {
					log.debug("Old status code of {} being set to 0", this.statusCode);
				}
				this.statusCode = 0;
			} catch (ArrayIndexOutOfBoundsException aiobe) {
				if (log.isDebugEnabled()) {
					log.debug(aiobe.getMessage());
					log.debug("Old status code of {} being set to 0", this.statusCode);
				}
				this.statusCode = 0;
			} catch(Exception e) {
				if (log.isDebugEnabled()) {
					log.debug(e.getMessage());
					log.debug("Old status code of {} being set to 0", this.statusCode);
				}
				this.statusCode = 0;
			}
			
			if (arr.length > 2) {
				this.statusMessage = arr[2].trim();
			} else {
				this.statusMessage = "";
			}

			if (StandardCharsets.US_ASCII.newEncoder().canEncode(statusLine)) {
				if (log.isDebugEnabled()) {
					log.debug(statusLine);
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("statusLine contains printable characters encountered");
				}
			}

			responseHeaders.loadFromStream(sockIn);
			length = NetUtils.getContentLength(responseHeaders);

			in = new FixedRangeInputStream(NetUtils.getInputStream(responseHeaders, socket.getInputStream()), length);

			StringBuilder b2 = new StringBuilder();
			responseHeaders.appendToBuffer(b2);
			if (reusing) {
				if (log.isDebugEnabled()) {
					log.debug("Socket reuse successfull");
				}
			}
			
			if (StandardCharsets.US_ASCII.newEncoder().canEncode(b2)) {
				if (log.isDebugEnabled()) {
					log.debug(b2.toString());
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("b2 contains non-printable characters");
				}
			}

			keepAliveSupported = !"close".equals(responseHeaders.getValue("connection"));

		} catch (HostUnreachableException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			throw new NetworkException("Unable to connect to server");
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			throw new NetworkException(e.getMessage());
		}
	}

	private void releaseSocket() {
		if (log.isDebugEnabled()) {
			log.debug("Releasing socket for reuse");
		}
		KeepAliveConnectionCache.getInstance().putSocket(socket, _url.getHost(), _url.getPort());
	}

	private Socket createSocket() throws IOException {
		Socket thisSocket = SocketFactory.createSocket(_url.getHost(), _url.getPort());
		if (_url.getProtocol().equalsIgnoreCase("https")) {
			thisSocket = SocketFactory.wrapSSL(thisSocket, _url.getHost(), _url.getPort());
		}
		return thisSocket;
	}

	@Override
	public long getContentLength() throws IOException {
		return length;
	}

	@Override
	public String getHost() {
		return _url.getHost() + ":" + _url.getPort();
	}
}
