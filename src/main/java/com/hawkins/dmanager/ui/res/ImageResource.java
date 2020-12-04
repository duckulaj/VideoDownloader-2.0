package com.hawkins.dmanager.ui.res;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

public class ImageResource {
	private final static String ICON_FOLDER = "icons";

	static Map<String, ImageIcon> iconMap = new HashMap<String, ImageIcon>();

	public static ImageIcon get(String id) {
		return get(id, true);
	}

	public static ImageIcon get(String id, boolean cacheResult) {
		ImageIcon icon = iconMap.get(id);
		if (icon == null) {
			icon = getIcon(id);
			if (icon != null && cacheResult) {
				iconMap.put(id, icon);
			}
		}
		return icon;
	}

	private static ImageIcon getIcon(String name) {
		try {
			java.net.URL url = ImageResource.class.getResource("/"
					+ ICON_FOLDER + "/" + name);
			if (url == null)
				throw new Exception();
			return new ImageIcon(url);
		} catch (Exception e) {
			return new ImageIcon(ICON_FOLDER + "/" + name);
		}
	}
}
