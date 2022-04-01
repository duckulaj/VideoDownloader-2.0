package com.hawkins.dmanager.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MacUtils {
	

	public static void open(final File f) throws FileNotFoundException {
		if (!f.exists()) {
			throw new FileNotFoundException();
		}
		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command("open", f.getAbsolutePath());
			if(pb.start().waitFor()!=0) {
				throw new FileNotFoundException();
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}

	public static void openFolder(String folder, String file) throws FileNotFoundException {
		File f = new File(folder, file);
		if (!f.exists()) {
			throw new FileNotFoundException();
		}
		try {
			ProcessBuilder pb = new ProcessBuilder();
			log.info("Opening folder: " + f.getAbsolutePath());
			pb.command("open", "-R", f.getAbsolutePath());
			if(pb.start().waitFor()!=0) {
				throw new FileNotFoundException();
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		}
		// try {
		// Runtime.getRuntime().exec(new String[] { "open -R \"" + f.getAbsolutePath() +
		// "\"" });
		// } catch (Exception e) {
		// log.info(e.getMessage());
		// }
	}

	public static boolean launchApp(String app, String args) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command("open", "-n", "-a", app, "--args", args);
			if(pb.start().waitFor()!=0) {
				throw new FileNotFoundException();
			}
			//Runtime.getRuntime().exec(new String[] { "open \"" + app + "\" " + args });
			return true;
		} catch (Exception e) {
			log.info(e.getMessage());
			return false;
		}
	}

	public static void keepAwakePing() {
		try {
			Runtime.getRuntime().exec("caffeinate -i -t 3");
		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}

	public static void addToStartup() {
		File dir = new File(System.getProperty("user.home"), "videoDownloader/Library/LaunchAgents");
		dir.mkdirs();
		File f = new File(dir, "org.sdg.dmanager.plist");
		FileOutputStream fs = null;
		try {
			fs = new FileOutputStream(f);
			fs.write(getStartupPlist().getBytes());
		} catch (Exception e) {
			log.info(e.getMessage());
		} finally {
			try {
				if (fs != null)
					fs.close();
			} catch (Exception e2) {
			}
		}
		f.setExecutable(true);
	}

	public static boolean isAlreadyAutoStart() {
		File f = new File(System.getProperty("user.home"), "videoDownloader/Library/LaunchAgents/org.sdg.dmanager.plist");
		if (!f.exists())
			return false;
		FileInputStream in = null;
		byte[] buf = new byte[(int) f.length()];
		try {
			in = new FileInputStream(f);
			if (in.read(buf) != f.length()) {
				return false;
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception e2) {
			}
		}
		String str = new String(buf);
		String s1 = getProperPath(System.getProperty("java.home"));
		String s2 = DManagerUtils.getJarFile().getAbsolutePath();
		return str.contains(s1) && str.contains(s2);
	}

	public static void removeFromStartup() {
		File f = new File(System.getProperty("user.home"), "videoDownloader/Library/LaunchAgents/org.sdg.dmanager.plist");
		f.delete();
	}

	public static String getStartupPlist() {
		String str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\"\r\n"
				+ "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\" >\r\n" + "<plist version=\"1.0\">\r\n"
				+ "	<dict>\r\n" + "		<key>Label</key>\r\n" + "		<string>org.sdg.dmanager</string>\r\n"
				+ "		<key>ProgramArguments</key>\r\n" + "		<array>\r\n"
				+ "			<string>%sbin/java</string>\r\n" + "			<string>-Xdock:name=DManager</string>\r\n"
				+ "			<string>-jar</string>\r\n"
				+ "			<!-- MODIFY THIS TO POINT TO YOUR EXECUTABLE JAR FILE -->\r\n"
				+ "			<string>%s</string>\r\n" + "			<string>-m</string>\r\n" + "		</array>\r\n"
				+ "		<key>OnDemand</key>\r\n" + "		<true />\r\n" + "		<key>RunAtLoad</key>\r\n"
				+ "		<true />\r\n" + "		<key>KeepAlive</key>\r\n" + "		<false />\r\n" + "	</dict>\r\n"
				+ "</plist>";
		String s1 = getProperPath(System.getProperty("java.home"));
		String s2 = DManagerUtils.getJarFile().getAbsolutePath();
		return String.format(str, s1, s2);
	}

	private static String getProperPath(String path) {
		if (path.endsWith("/"))
			return path;
		return path + "/";
	}
	
	public static void browseURL(final String url) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command("open", url);
			pb.start();// .waitFor();
		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}
}
