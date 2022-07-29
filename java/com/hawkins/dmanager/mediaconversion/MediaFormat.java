package com.hawkins.dmanager.mediaconversion;

import com.hawkins.dmanager.ui.res.StringResource;


public class MediaFormat {
	public MediaFormat() {
	}

	public MediaFormat(int width, int height, String format,
			String description, boolean audioOnly) {
		this.width = width;
		this.height = height;
		this.format = format;
		this.description = description;
		this.audioOnly = audioOnly;
	}

	public MediaFormat(int width, int height, String format, String description) {
		this(width, height, format, description, false);
	}

	private int width, height;
	private String format, description;
	private boolean audioOnly;

	public final int getWidth() {
		return width;
	}

	public final void setWidth(int width) {
		this.width = width;
	}

	public final int getHeight() {
		return height;
	}

	public final void setHeight(int height) {
		this.height = height;
	}

	public final String getFormat() {
		return format;
	}

	public final void setFormat(String format) {
		this.format = format;
	}

	public final String getDescription() {
		return description;
	}

	public final void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		if (width < 0) {
			return StringResource.get("VID_FMT_ORIG");
		}
		if (audioOnly) {
			return format + " " + description + " " + width + "kbps ";
		}
		return format + " " + description + " (" + width + "x" + height + ") ";
	}

	public final boolean isAudioOnly() {
		return audioOnly;
	}

	public final void setAudioOnly(boolean audioOnly) {
		this.audioOnly = audioOnly;
	}
}
