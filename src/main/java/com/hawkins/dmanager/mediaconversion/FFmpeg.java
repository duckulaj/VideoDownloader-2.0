package com.hawkins.dmanager.mediaconversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.hawkins.dmanager.Config;
import com.hawkins.dmanager.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FFmpeg {
	

	public final static int FF_NOT_FOUND = 10, FF_LAUNCH_ERROR = 20, FF_CONVERSION_FAILED = 30, FF_SUCCESS = 0;
	private MediaFormat outformat;
	private MediaConversionListener listener;
	private boolean copy;
	private List<String> inputFiles;
	private String outputFile;
	private boolean hls;
	private long totalDuration = 0;
	private Process proc;
	private int ffExitCode;

	public FFmpeg(List<String> inputFiles, String outputFile, MediaConversionListener listener, MediaFormat outformat,
			boolean copy) {
		this.inputFiles = inputFiles;
		this.outputFile = outputFile;
		this.listener = listener;
		this.outformat = outformat;
		this.copy = copy;
	}

	public int convert() {
		try {

			log.info("Outformat: " + outformat + " audio: " + outformat.isAudioOnly());

			File ffFile = new File(Config.getInstance().getDataFolder(),
					System.getProperty("os.name").toLowerCase().contains("windows") ? "ffmpeg.exe" : "ffmpeg");
			if (!ffFile.exists()) {
				return FF_NOT_FOUND;
			}

			List<String> args = new ArrayList<String>();
			args.add(ffFile.getAbsolutePath());

			if (hls) {
				args.add("-f");
				args.add("concat");
				args.add("-safe");
				args.add("0");
			}

			for (int i = 0; i < inputFiles.size(); i++) {
				args.add("-i");
				args.add(inputFiles.get(i));
			}

			if (outformat.isAudioOnly()) {
				if (outformat.getWidth() > 0) {
					args.add("-b:a");
					args.add(outformat.getWidth() + "k");
				} else if (copy) {
					args.add("-acodec");
					args.add("copy");
				}
			} else {
				if (outformat.getWidth() > 0) {
					args.add("-vf");
					args.add("scale=" + outformat.getWidth() + ":" + outformat.getHeight());
					// args.add("scale=w=" + outformat.getWidth() + ":h=" +
					// outformat.getHeight()
					// + ":force_original_aspect_ratio=decrease");
				} else if (copy) {
					args.add("-acodec");
					args.add("copy");
					args.add("-vcodec");
					args.add("copy");
				}
			}

			args.add(outputFile);
			args.add("-y");

			for (String s : args) {
				log.info("@ffmpeg_args: " + s);
			}

			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream(true);
			proc = pb.start();

			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()), 1024);
			while (true) {
				String ln = br.readLine();
				if (ln == null) {
					break;
				}
				try {
					String text = ln.trim();
					processOutput(text);
				} catch (Exception e) {
					log.info(e.getMessage());
				}
			}

			ffExitCode = proc.waitFor();
			return ffExitCode == 0 ? FF_SUCCESS : FF_CONVERSION_FAILED;
		} catch (Exception e) {
			return FF_LAUNCH_ERROR;
		}
	}

	public void setHls(boolean hls) {
		this.hls = hls;
	}

	public void setHLSDuration(float totalDuration) {
		this.totalDuration = (long) totalDuration;
	}

	private long parseDuration(String dur) {
		long duration = 0;
		String[] arr = dur.split(":");
		String s = arr[0].trim();
		if (!StringUtils.isNullOrEmpty(s)) {
			duration = Integer.parseInt(s, 10) * 3600;
		}
		s = arr[1].trim();
		if (!StringUtils.isNullOrEmpty(s)) {
			duration += Integer.parseInt(arr[1].trim(), 10) * 60;
		}
		s = arr[2].split("\\.")[0].trim();
		if (!StringUtils.isNullOrEmpty(s)) {
			duration += Integer.parseInt(s, 10);
		}
		return duration;
	}

	private void processOutput(String text) {
		if (StringUtils.isNullOrEmpty(text)) {
			return;
		}
		if (totalDuration > 0) {
			if (text.startsWith("frame=") && text.contains("time=")) {
				int index1 = text.indexOf("time");
				index1 = text.indexOf('=', index1);
				int index2 = text.indexOf("bitrate=");
				String dur = text.substring(index1 + 1, index2).trim();
				log.info("Parsing duration: " + dur);
				long t = parseDuration(dur);
				log.info("Duration: " + t + " Total duration: " + totalDuration);
				int prg = (int) ((t * 100) / totalDuration);
				log.info("ffmpeg prg: " + prg);
				listener.progress(prg);
			}
		}

		if (totalDuration == 0) {
			if (text.startsWith("Duration:")) {
				try {
					int index1 = text.indexOf("Duration");
					index1 = text.indexOf(':', index1);
					int index2 = text.indexOf(",", index1);
					String dur = text.substring(index1 + 1, index2).trim();
					log.info("Parsing duration: " + dur);
					totalDuration = parseDuration(dur);
					log.info("Total duration: " + totalDuration);
				} catch (Exception e) {
					log.info(e.getMessage());
					totalDuration = -1;
				}
			}
		}
	}

	public void stop() {
		try {
			if (proc != null) {
				proc.destroy();
			}
		} catch (Exception e) {
		}
	}

	public int getFfExitCode() {
		return ffExitCode;
	}
}
