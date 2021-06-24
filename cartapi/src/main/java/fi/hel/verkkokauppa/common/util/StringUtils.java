package fi.hel.verkkokauppa.common.util;

public final class StringUtils {

	public static boolean isEmpty(String string) {
		return (string == null || string.trim().isEmpty());
	}

	public static boolean isEmptyOrWhitespace(String string) {
		return (string == null || string.trim().isEmpty());
	}

	public static boolean isNotEmpty(String string) {
		return (string != null && !string.isEmpty());
	}

	public static String emptyToNull(String string) {
		return ((string != null && !string.isEmpty()) ? string : null);
	}
}
