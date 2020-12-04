package com.hawkins.dmanager.win32;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hawkins.dmanager.util.DManagerUtils;

public class NativeMethods {
	
	private static final Logger logger = LogManager.getLogger(NativeMethods.class.getName());

	private static NativeMethods _me;

	public static NativeMethods getInstance() {
		if (_me == null) {
			_me = new NativeMethods();
		}
		return _me;
	}

	private NativeMethods() {
		String dllPath = new File(DManagerUtils.getJarFile().getParentFile(), "xdm_native.dll").getAbsolutePath();
		try {
			System.load(dllPath);
		} catch (Exception e) {
			logger.info(e);
		}
	}

	public final native void keepAwakePing();

	public final native void addToStartup(String key, String value);

	public final native boolean presentInStartup(String key, String value);

	public final native void removeFromStartup(String key);
	
	public final native String getDownloadsFolder();
	
	public final native String stringTest(String str);
}
