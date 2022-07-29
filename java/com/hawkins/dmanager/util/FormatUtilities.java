package com.hawkins.dmanager.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.hawkins.dmanager.DownloadEntry;
import com.hawkins.dmanager.DownloadQueue;
import com.hawkins.dmanager.QueueManager;
import com.hawkins.dmanager.DManagerConstants;
import com.hawkins.dmanager.ui.res.StringResource;
import com.hawkins.utils.Utils;

public class FormatUtilities {
	private static SimpleDateFormat _format;
	private static final int MB = 1024 * 1024, KB = 1024;

	public static String formatDate(long date) {
		if (_format == null) {
			_format = new SimpleDateFormat("yyyy-MM-dd");
		}
		Date dt = new Date(date);
		return _format.format(dt);
	}

	public static String formatSize(double length) {
		if (length < 0)
			return "---";
		if (length > MB) {
			return String.format("%.1f MB", (float) length / MB);
		} else if (length > KB) {
			return String.format("%.1f KB", (float) length / KB);
		} else {
			return String.format("%d B", (int) length);
		}
	}

	public static String getFormattedStatus(DownloadEntry ent) {
		String statStr = "";
		if (ent.getQueueId() != null) {
			DownloadQueue q = QueueManager.getInstance().getQueueById(ent.getQueueId());
			String qname = "";
			if (q != null && q.getQueueId() != null) {
				qname = q.getQueueId().length() > 0 ? "[ " + q.getName() + " ] " : "";
			}
			statStr += qname;
		}

		if (ent.getState() == DManagerConstants.FINISHED) {
			statStr += StringResource.get("STAT_FINISHED");
		} else if (ent.getState() == DManagerConstants.PAUSED || ent.getState() == DManagerConstants.FAILED) {
			statStr += StringResource.get("STAT_PAUSED");
		} else if (ent.getState() == DManagerConstants.ASSEMBLING) {
			statStr += StringResource.get("STAT_ASSEMBLING");
		} else {
			statStr += StringResource.get("STAT_DOWNLOADING");
		}
		String sizeStr = formatSize(ent.getSize());
		if (ent.getState() == DManagerConstants.FINISHED) {
			return statStr + " " + sizeStr;
		} else {
			if (ent.getSize() > 0) {
				String downloadedStr = formatSize(ent.getDownloaded());
				// String progressStr = ent.getProgress() + "%";
				String downloadSpeed = Utils.format(ent.getDownloadSpeed(), 2);
				return statStr + " [ " + downloadedStr + " / " + sizeStr + " ] " + downloadSpeed + "/s : ETA " + ent.getEta();
			} else {
				return statStr + (ent.getProgress() > 0 ? (" " + ent.getProgress() + "%") : "")
						+ (ent.getDownloaded() > 0 ? " " + formatSize(ent.getDownloaded())
								: (ent.getState() == DManagerConstants.PAUSED || ent.getState() == DManagerConstants.FAILED ? ""
										: " ..."));
			}
		}
	}

	public static String getETA(double length, float rate) {
		if (length == 0)
			return "00:00:00";
		if (length < 1 || rate <= 0)
			return "---";
		int sec = (int) (length / rate);
		return hms(sec);
	}

	public static String hms(int sec) {
		int hrs = 0, min = 0;
		hrs = sec / 3600;
		min = (sec % 3600) / 60;
		sec = sec % 60;
		String str = String.format("%02d:%02d:%02d", hrs, min, sec);
		return str;
	}
}
