package com.hawkins.dmanager;

import com.hawkins.dmanager.downloaders.metadata.HttpMetadata;

public interface LinkRefreshCallback {
	public String getId();

	public boolean isValidLink(HttpMetadata metadata);
}
