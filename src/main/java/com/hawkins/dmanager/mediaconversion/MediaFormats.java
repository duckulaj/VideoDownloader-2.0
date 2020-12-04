package com.hawkins.dmanager.mediaconversion;

public class MediaFormats {
	private static MediaFormat[] supportedFormats;
	static {
		supportedFormats = new MediaFormat[11];
		supportedFormats[0] = new MediaFormat(-1, -1, null, null);
		supportedFormats[1] = new MediaFormat(1366, 768, "MP4", "Video");
		supportedFormats[2] = new MediaFormat(1920, 1080, "MP4", "Video");
		supportedFormats[3] = new MediaFormat(1280, 800, "MP4", "Video");
		supportedFormats[4] = new MediaFormat(320, 568, "MP4", "Video");
		supportedFormats[5] = new MediaFormat(320, 480, "MP4", "Video");
		supportedFormats[6] = new MediaFormat(1920, 1200, "MP4", "Video");
		supportedFormats[7] = new MediaFormat(720, 1280, "MP4", "Video");
		supportedFormats[8] = new MediaFormat(96, -1, "MP3", "Audio", true);
		supportedFormats[9] = new MediaFormat(128, -1, "MP3", "Audio", true);
		supportedFormats[10] = new MediaFormat(320, -1, "MP3", "Audio", true);
	}

	public MediaFormats() {

	}

	public static final MediaFormat[] getSupportedFormats() {
		return supportedFormats;
	}

	public static final void setSupportedFormats(MediaFormat[] supportedFmts) {
		supportedFormats = supportedFmts;
	}

}
