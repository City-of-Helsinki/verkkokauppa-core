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

	public static String removeLast(String value, int countToRemove) throws StringIndexOutOfBoundsException{

		int strLength = value.length();

		if (countToRemove > strLength) {
			throw new StringIndexOutOfBoundsException("Number of character to remove from end is greater than the length of the string");
		} else if (!value.isEmpty()) {
			value = value.substring(0, value.length() - countToRemove);
		}
		return value;
	}
}
