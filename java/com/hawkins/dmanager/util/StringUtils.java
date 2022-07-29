package com.hawkins.dmanager.util;

public class StringUtils {
	
	private StringUtils() {
	    throw new IllegalStateException("Utility class");
	}
	
	public static boolean isNullOrEmpty(String str) {
		return str == null || str.length() < 1;
	}

	public static boolean isNullOrEmptyOrBlank(String str) {
		return str == null || str.trim().length() < 1;
	}

	public static byte[] getBytes(StringBuffer sb) {
		return sb.toString().getBytes();
	}

	public static byte[] getBytes(StringBuilder sb) {
		return sb.toString().getBytes();
	}

	public static byte[] getBytes(String s) {
		return s.getBytes();
	}
}
