package com.hawkins.dmanager.util;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Locale;

import com.hawkins.SpringVideoDownloadApplication;
import com.hawkins.dmanager.Config;
import com.hawkins.dmanager.DManagerConstants;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DManagerUtils {
	

	private static final char[] invalid_chars = { '/', '\\', '"', '?', '*', '<', '>', ':', '|' };

	public static String decodeFileName(String str) {
		char ch[] = str.toCharArray();
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < ch.length; i++) {
			if (ch[i] == '/' || ch[i] == '\\' || ch[i] == '"' || ch[i] == '?' || ch[i] == '*' || ch[i] == '<'
					|| ch[i] == '>' || ch[i] == ':')
				continue;
			if (ch[i] == '%') {
				if (i + 2 < ch.length) {
					int c = Integer.parseInt(ch[i + 1] + "" + ch[i + 2], 16);
					buf.append((char) c);
					i += 2;
					continue;
				}
			}
			buf.append(ch[i]);
		}
		return buf.toString();
	}

	public static String getFileName(String uri) {
		try {
			if (uri == null)
				return "FILE";
			if (uri.equals("/") || uri.length() < 1) {
				return "FILE";
			}
			int x = uri.lastIndexOf("/");
			String path = uri;
			if (x > -1) {
				path = uri.substring(x);
			}
			int qindex = path.indexOf("?");
			if (qindex > -1) {
				path = path.substring(0, qindex);
			}
			path = decodeFileName(path);
			if (path.length() < 1)
				return "FILE";
			if (path.equals("/"))
				return "FILE";
			return createSafeFileName(path);
		} catch (Exception e) {
			log.info(e.getMessage());
			return "FILE";
		}
	}

	private static String createSafeFileName(String str) {
		String safe_name = str;
		for (int i = 0; i < invalid_chars.length; i++) {
			if (safe_name.indexOf(invalid_chars[i]) != -1) {
				safe_name = safe_name.replace(invalid_chars[i], '_');
			}
		}
		return safe_name;
	}

	static String doc[] = { ".doc", ".docx", ".txt", ".pdf", ".rtf", ".xml", ".c", ".cpp", ".java", ".cs", ".vb",
			".html", ".htm", ".chm", ".xls", ".xlsx", ".ppt", ".pptx", ".js", ".css" };
	static String cmp[] = { ".7z", ".zip", ".rar", ".gz", ".tgz", ".tbz2", ".bz2", ".lzh", ".sit", ".z" };
	static String music[] = { ".mp3", ".wma", ".ogg", ".aiff", ".au", ".mid", ".midi", ".mp2", ".mpa", ".wav", ".aac",
			".oga", ".ogx", ".ogm", ".spx", ".opus" };
	static String vid[] = { ".mpg", ".mpeg", ".avi", ".flv", ".asf", ".mov", ".mpe", ".wmv", ".mkv", ".mp4", ".3gp",
			".divx", ".vob", ".webm", ".ts" };
	static String prog[] = { ".exe", ".msi", ".bin", ".sh", ".deb", ".cab", ".cpio", ".dll", ".jar", "rpm", ".run",
			".py" };

	public static int findCategory(String filename) {
		String file = filename.toLowerCase();
		for (int i = 0; i < doc.length; i++) {
			if (file.endsWith(doc[i])) {
				return DManagerConstants.DOCUMENTS;
			}
		}
		for (int i = 0; i < cmp.length; i++) {
			if (file.endsWith(cmp[i])) {
				return DManagerConstants.COMPRESSED;
			}
		}
		for (int i = 0; i < music.length; i++) {
			if (file.endsWith(music[i])) {
				return DManagerConstants.MUSIC;
			}
		}
		for (int i = 0; i < prog.length; i++) {
			if (file.endsWith(prog[i])) {
				return DManagerConstants.PROGRAMS;
			}
		}
		for (int i = 0; i < vid.length; i++) {
			if (file.endsWith(vid[i])) {
				return DManagerConstants.VIDEO;
			}
		}
		return DManagerConstants.OTHER;
	}

	public static String appendArray2Str(String[] arr) {
		boolean first = true;
		StringBuilder buf = new StringBuilder();
		for (String s : arr) {
			if (!first) {
				buf.append(",");
			}
			buf.append(s);
			first = false;
		}
		return buf.toString();
	}

	public static String getExtension(String file) {
		int index = file.lastIndexOf(".");
		if (index > 0) {
			String ext = file.substring(index).toLowerCase();
			return ext;
		} else {
			return null;
		}
	}

	public static String getFileNameWithoutExtension(String fileName) {
		int index = fileName.lastIndexOf(".");
		if (index > 0) {
			fileName = fileName.substring(0, index).toLowerCase();
			return fileName;
		} else {
			return fileName;
		}
	}

	public static void copyStream(InputStream instream, OutputStream outstream, long size) throws Exception {
		byte[] b = new byte[8192];
		long rem = size;
		while (true) {
			int bs = (int) (size > 0 ? (rem > b.length ? b.length : rem) : b.length);
			int x = instream.read(b, 0, bs);
			if (x == -1) {
				if (size > 0) {
					throw new EOFException("Unexpected EOF");
				} else {
					break;
				}
			}
			outstream.write(b, 0, x);
			rem -= x;
			if (size > 0) {
				if (rem <= 0)
					break;
			}
		}
	}

	public static final int WINDOWS = 10, MAC = 20, LINUX = 30;

	public static final int detectOS() {
		String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
		if (os.contains("mac") || os.contains("darwin") || os.contains("os x") || os.contains("os x")) {
			return MAC;
		} else if (os.contains("linux")) {
			return LINUX;
		} else if (os.contains("windows")) {
			return WINDOWS;
		} else {
			return -1;
		}
	}

	public static final int getOsArch() {
		if (System.getProperty("os.arch").contains("64")) {
			return 64;
		} else {
			return 32;
		}
	}

	public static boolean exec(String args) {
		try {
			log.info("Launching: " + args);
			Runtime.getRuntime().exec(args);
		} catch (IOException e) {
			log.info(e.getMessage());
			return false;
		}
		return true;
	}

	public static long getFreeSpace(String folder) {
		if (folder == null)
			return new File(Config.getInstance().getTemporaryFolder()).getFreeSpace();
		else
			return new File(folder).getFreeSpace();
	}

	public static void keepAwakePing() {
		try {
			int os = detectOS();
			if (os == LINUX) {
				LinuxUtils.keepAwakePing();
			} else if (os == WINDOWS) {
				WinUtils.keepAwakePing();
			} else if (os == MAC) {
				MacUtils.keepAwakePing();
			}
		} catch (Throwable e) {
			log.info(e.getMessage());
		}
	}

	public static boolean isAlreadyAutoStart() {
		try {
			int os = detectOS();
			if (os == LINUX) {
				return LinuxUtils.isAlreadyAutoStart();
			} else if (os == WINDOWS) {
				return WinUtils.isAlreadyAutoStart();
			} else if (os == MAC) {
				return MacUtils.isAlreadyAutoStart();
			}
			return false;
		} catch (Throwable e) {
			log.info(e.getMessage());
		}
		return false;
	}

	public static void addToStartup() {
		try {
			int os = detectOS();
			if (os == LINUX) {
				LinuxUtils.addToStartup();
			} else if (os == WINDOWS) {
				WinUtils.addToStartup();
			} else if (os == MAC) {
				MacUtils.addToStartup();
			}
		} catch (Throwable e) {
			log.info(e.getMessage());
		}
	}

	public static File getJarFile() {
		try {
			return new File(SpringVideoDownloadApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static boolean below7() {
		try {
			int version = Integer.parseInt(System.getProperty("os.version").split("\\.")[0]);
			return (version < 6);
		} catch (Exception e) {

		}
		return false;
	}
	
	public static String getDownloadsFolder() {
		return new File(System.getProperty("user.home"), "videoDownloader/Downloads").getAbsolutePath();
	}
}
