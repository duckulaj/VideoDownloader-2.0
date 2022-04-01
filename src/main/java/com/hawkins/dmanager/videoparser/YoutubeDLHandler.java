package com.hawkins.dmanager.videoparser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.hawkins.dmanager.Config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YoutubeDLHandler {
	

	private Process proc;
	private int exitCode;
	private String url;
	private String ydlLocation;
	private String title;

	private ArrayList<VideoDownloaderResponse.YdlVideo> videos;

	public YoutubeDLHandler(String url) {
		this.url = url;
		this.videos = new ArrayList<>();
		ydlLocation = new File(Config.getInstance().getDataFolder(),
				System.getProperty("os.name").toLowerCase().contains("windows") ? "youtube-dl.exe" : "youtube-dl")
						.getAbsolutePath();
	}

	public void start() {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream(8192);
			List<String> args = new ArrayList<String>();
			args.add(ydlLocation);
			args.add("--no-warnings");
			args.add("-J");
			//args.add("--proxy");
			//args.add("http://127.0.0.1:8888");
			args.add(url);

			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream(true);
			proc = pb.start();

			InputStream in = proc.getInputStream();
			byte[] buf = new byte[8192];
			while (true) {
				int x = in.read(buf);
				if (x == -1)
					break;
				bout.write(buf, 0, x);
			}

			// OutputStream out = new FileOutputStream("test.txt");
			// out.write(bout.toByteArray());
			// out.close();

			String json = new String(bout.toByteArray());
			System.out.println("----json: " + json);
			System.out.println("----json end ----");
			exitCode = proc.waitFor();
			if (exitCode == 0) {
				StringBuilder title = new StringBuilder();
				videos.addAll(VideoDownloaderResponse.parse(new ByteArrayInputStream(bout.toByteArray())));
				this.title = title.toString();
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		}

	}

	public int getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ArrayList<VideoDownloaderResponse.YdlVideo> getVideos() {
		return videos;
	}

	public void stop() {
		try {
			proc.destroy();
		} catch (Exception e) {
			log.info(e.getMessage());
		}
	}
}
