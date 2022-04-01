package com.hawkins.dmanager.downloaders;

import java.io.EOFException;
import java.io.InputStream;

import com.hawkins.dmanager.downloaders.http.HttpChannel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractChannel implements Runnable {
	

	protected Segment chunk;
	private InputStream in;
	private byte[] buf;
	protected volatile boolean stop;
	protected String errorMessage;
	private boolean closed;
	private Thread t;
	protected int errorCode;

	protected AbstractChannel(Segment chunk) {
		this.chunk = chunk;
		buf = new byte[8192];
	}

	public void open() {
		t = new Thread(this);
		t.setName(this.chunk.getId());
		t.start();
	}

	protected abstract boolean connectImpl();

	protected abstract InputStream getInputStreamImpl();

	protected abstract long getLengthImpl();

	protected abstract void closeImpl();

	private boolean connect() {
		try {
			chunk.getChunkListener().synchronize();
		} catch (NullPointerException e) {
			if (log.isDebugEnabled()) {
				log.debug("stopped chunk {}", chunk);
			}
			return false;
		}
		if (connectImpl()) {
			in = getInputStreamImpl();
			long length = getLengthImpl();
			if (chunk.getLength() < 0) {
				if (log.isDebugEnabled()) {
					log.debug("Setting length of {} to ", chunk.getId(), length);
				}
				chunk.setLength(length);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void run() {
		try {
			while (!stop) {
				if (!connect()) {
					if (!stop) {
						
						if (errorMessage == null) errorMessage = "AbstractChannel().run :: errorMessage is null";
						
						if (chunk != null) {
							chunk.transferFailed(errorMessage);
						}
					}
					close();
					break;
				}
				chunk.transferInitiated();
				// do not proceed if chunk is stoppped
				if (chunk == null) {
					continue;
				}
				if (((chunk.getLength() > 0) ? copyStream1() : copyStream2())) {
					break;
				}
			}
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("Internal problem: {}", e);
			}
			if (!stop) {
				if (errorMessage == null) errorMessage = "AbstractChannel().run :: errorMessage is null";
				chunk.transferFailed(errorMessage);
			}
		} finally {
			close();
		}
	}

	private void close() {
		if (closed)
			return;
		closeImpl();
		closed = true;
	}

	public void stop() {
		stop = true;
		this.chunk = null;
		if (this.t != null) {
			t.interrupt();
		}
	}

	private boolean copyStream1() {
		try {
			while (!stop) {
				chunk.getChunkListener().synchronize();
				long rem = chunk.getLength() - chunk.getDownloaded();
				if (rem == 0) {
					if (this instanceof HttpChannel) {
						if (((HttpChannel) this).isFinished()) {
							close();
						}
					} else {
						close();
					}
					if (chunk.transferComplete()) {
						if (log.isDebugEnabled()) {
							log.debug("{} complete and closing {} {}",chunk, chunk.getDownloaded(), chunk.getLength());
						}
						return true;
					}
				}
				if (stop) {
					return false;
				}

				int diff = (int) (rem > buf.length ? buf.length : rem);

				int x = in.read(buf, 0, diff);
				if (stop)
					return false;
				if (x == -1) {
					throw new EOFException("Unexpected eof");
				}
				chunk.getOutStream().write(buf, 0, x);
				if (stop)
					return false;
				chunk.setDownloaded(chunk.getDownloaded() + x);
				chunk.transferring();
			}
			return false;
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			return false;
		} finally {
			close();
		}
	}

	private boolean copyStream2() {
		try {
			while (!stop) {
				chunk.getChunkListener().synchronize();
				int x = in.read(buf, 0, buf.length);
				if (stop)
					return false;
				if (x == -1) {
					chunk.transferComplete();
					return true;
				}
				chunk.getOutStream().write(buf, 0, x);
				if (stop)
					return false;
				chunk.setDownloaded(chunk.getDownloaded() + x);
				chunk.transferring();
			}
			return false;
		} catch (Exception e) {
			return false;
		} finally {
			close();
		}
	}

	public int getErrorCode() {
		return errorCode;
	}

}
