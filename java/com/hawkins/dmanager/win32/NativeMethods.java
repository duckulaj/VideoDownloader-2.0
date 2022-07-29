package com.hawkins.dmanager.win32;

import java.io.File;

import com.hawkins.dmanager.util.DManagerUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NativeMethods {
	

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
			log.info(e.getMessage());
		}
	}

	public final native void keepAwakePing();

	public final native void addToStartup(String key, String value);

	public final native boolean presentInStartup(String key, String value);

	public final native void removeFromStartup(String key);
	
	public final native String getDownloadsFolder();
	
	public final native String stringTest(String str);
}
