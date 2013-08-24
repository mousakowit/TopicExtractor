package org.vskm.text.topic.util;

public class StringUtil {
	
	public static boolean isShingle(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		return (s.split("\\s").length > 1);
	}

}
