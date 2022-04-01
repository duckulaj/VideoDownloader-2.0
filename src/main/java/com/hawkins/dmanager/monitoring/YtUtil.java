package com.hawkins.dmanager.monitoring;

import java.util.ArrayList;
import java.util.HashMap;

import com.hawkins.dmanager.network.http.HeaderCollection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YtUtil {
	

	static DASH_INFO lastVid;

	public static boolean isNormalVideo(int itag) {
		return ((itag > 4 && itag < 79) || (itag > 81 && itag < 86) || (itag > 99 && itag < 103));
	}

	static Object lockObject = new Object();

	static ArrayList<DASH_INFO> videoQueue = new ArrayList<DASH_INFO>(), audioQueue = new ArrayList<DASH_INFO>();

	public static boolean addToQueue(DASH_INFO info) {
		synchronized (lockObject) {
			if(videoQueue.size()>32) {
				videoQueue.remove(0);
			}
			if(audioQueue.size()>32) {
				audioQueue.remove(0);
			}
			if (info.video) {
				for (int i = videoQueue.size() - 1; i >= 0; i--) {
					DASH_INFO di = videoQueue.get(i);
					if (di.clen == info.clen) {
						if (di.id.equals(info.id)) {
							return false;
						}
					}
				}
				videoQueue.add(info);
				log.info("video added " + videoQueue.size());

				return true;
			} else {
				for (int i = audioQueue.size() - 1; i >= 0; i--) {
					DASH_INFO di = audioQueue.get(i);
					if (di.clen == info.clen) {
						if (di.id.equals(info.id)) {
							return false;
						}
					}
				}
				audioQueue.add(info);
				log.info("added added " + audioQueue.size());
				return true;
			}
		}
	}

	public static DASH_INFO getDASHPair(DASH_INFO info) {
		synchronized (lockObject) {
			if (info.video) {
				if (audioQueue.size() < 1)
					return null;
				for (int i = audioQueue.size() - 1; i >= 0; i--) {
					DASH_INFO di = audioQueue.get(i);
					if (di.id.equals(info.id)) {
						log.info("found matching audio");
						return di;
					}
				}
			} else {
				if (videoQueue.size() < 1)
					return null;
				for (int i = videoQueue.size() - 1; i >= 0; i--) {
					DASH_INFO di = videoQueue.get(i);
					if (di.id.equals(info.id)) {
						if ((lastVid != null) && (lastVid.clen == di.clen)) {
							return null;
						}
						lastVid = di;
						log.info("found matching video");
						return di;
					}
				}
			}
			return null;
		}
	}

	static HashMap<Integer, String> itags;

	public static String getInfoFromITAG(int itag) {
		loadITAGs();
		return (String) itags.get(itag);
	}

	static void loadITAGs() {
		if (itags != null) {
			return;
		}
		itags = new HashMap<Integer, String>();
		itags.put(5, "320 x 240");
		itags.put(6, "450 x 270");
		itags.put(13, "Small");
		itags.put(17, "176 x 144");
		itags.put(18, "480 x 360");
		itags.put(22, "1280 x 720");
		itags.put(34, "480 x 360");
		itags.put(35, "640 x 480 ");
		itags.put(36, "320 x 240");
		itags.put(37, "1920 x 1080");
		itags.put(38, "2048 x 1080");
		itags.put(43, "480 x 360");
		itags.put(44, "640 x 480");
		itags.put(45, "1280 x 720");
		itags.put(46, "1920 x 1080");
		itags.put(59, "854 x 480");
		itags.put(78, "854 x 480");
		itags.put(82, "480 x 360   3D");
		itags.put(83, "640 x 480   3D");
		itags.put(84, "1280 x 720   3D");
		itags.put(85, "1920 x 1080  3D");
		itags.put(100, "480 x 360   3D");
		itags.put(101, "640 x 480   3D");
		itags.put(102, "1280 x 720   3D");
		itags.put(133, "320 x 240");
		itags.put(134, "480 x 360");
		itags.put(135, "640 x 480");
		itags.put(136, "1280 x 720");
		itags.put(137, "1920 x 1080");
		itags.put(139, "Low bitrate");
		itags.put(140, "Med bitrate");
		itags.put(141, "Hi  bitrate");
		itags.put(160, "256 x 144");
		itags.put(167, "360p");
		itags.put(168, "480p");
		itags.put(169, "720p");
		itags.put(170, "1080p");
		itags.put(171, "Med bitrate");
		itags.put(172, "Hi  bitrate");
		itags.put(218, "480");
		itags.put(219, "480");
		itags.put(242, "320 x 240");
		itags.put(243, "480 x 360");
		itags.put(244, "640 x 480");
		itags.put(245, "640 x 480");
		itags.put(246, "640 x 480");
		itags.put(247, "1280 x 720");
		itags.put(248, "1920 x 1080");
		itags.put(264, "1440p");
		itags.put(266, "2160p");
		itags.put(271, "1440p");
		itags.put(272, "2160p");
		itags.put(278, "144p");
		itags.put(298, "720p");
		itags.put(302, "720p");
		itags.put(303, "1080p");
		itags.put(308, "1440p");
		itags.put(313, "2160p");
		itags.put(315, "2160p");
		itags.put(299, "2160p");
	}

}

class DASH_INFO {
	public String url;
	public long clen;
	public boolean video;
	public String id;
	public int itag;
	public String mime;
	public HeaderCollection headers;
}
