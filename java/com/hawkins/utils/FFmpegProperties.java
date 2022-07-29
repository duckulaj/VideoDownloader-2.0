package com.hawkins.utils;

import java.io.IOException;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;

public class FFmpegProperties  {

	private static FFmpegProperties thisInstance = null;
	private FFmpeg ffmpeg; 
	private FFprobe ffprobe;
	
	public static synchronized FFmpegProperties getInstance() throws IOException
	{
		if (FFmpegProperties.thisInstance == null) {
		
			FFmpegProperties.thisInstance = new FFmpegProperties();
		
		}
		
		return  FFmpegProperties.thisInstance;
	}
	
	public FFmpegProperties() throws IOException {
		
		this.ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
		this.ffprobe = new FFprobe("/usr/bin/ffprobe");
	}

	public FFmpeg getFfmpeg() {
		return this.ffmpeg;
	}

	public FFprobe getFfprobe() {
		return this.ffprobe;
	}
	
	
}
