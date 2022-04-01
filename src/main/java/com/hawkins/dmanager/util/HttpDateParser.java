package com.hawkins.dmanager.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpDateParser {
	

	private static SimpleDateFormat fmt;

	public static Date parseHttpDate(String lastModified) {
		if (StringUtils.isNullOrEmptyOrBlank(lastModified)) {
			return null;
		}
		if (fmt == null) {
			fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.UK);
			fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		}
		try {
			return fmt.parse(lastModified);
		} catch (ParseException e) {
			log.info(e.getMessage());
		}
		return null;
	}
}
