package fi.hel.verkkokauppa.utils;

import java.util.Arrays;
import java.util.Collection;

public final class AssertUtils {

	public static void assertTrue(boolean value, String name) {
		if (!value) {
			throw new IllegalStateException("Value [" + name + "] must be true");
		}
	}

	public static void assertFalse(boolean value, String name) {
		if (value) {
			throw new IllegalStateException("Value [" + name + "] must be false");
		}
	}

	public static void assertArgumentNotNull(Object value, String name) {
		if (value == null) {
			throw new IllegalArgumentException("Value of argument [" + name + "] may not be null");
		}
	}

	public static void assertPropertyNotNull(Object value, String name) {
		if (value == null) {
			throw new IllegalStateException("Value of property [" + name + "] may not be null");
		}
	}

	public static void assertArgumentPropertyNotNull(Object value, String propertyName, String argumentName) {
		if (value == null) {
			throw new IllegalArgumentException("Value of property [" + propertyName + "] of argument [" + argumentName + "] may not be null");
		}
	}

	public static void assertResultNotNull(Object value, String name) {
		if (value == null) {
			throw new IllegalStateException("Value of result [" + name + "] may not be null");
		}
	}

	public static void assertResultNotEmpty(Collection<?> value, String name) {
		if (value == null || value.isEmpty()) {
			throw new IllegalStateException("Value of result [" + name + "] may not be null or empty");
		}
	}

	public static void assertResultPropertyNotNull(Object value, String name, String resultName) {
		if (value == null) {
			throw new IllegalArgumentException("Value of property [" + name + "] of result [" + resultName + "] may not be null");
		}
	}

	public static void assertArgumentNotEmpty(String value, String name) {
		if (value == null) {
			throw new IllegalArgumentException("Value of argument [" + name + "] may not be null");
		}
		if (value.isEmpty()) {
			throw new IllegalArgumentException("Value of argument [" + name + "] may not be empty");
		}
	}

	public static void assertArgumentNotEmpty(Collection<?> elements, String name) {
		if (elements == null) {
			throw new IllegalArgumentException("Collection in argument [" + name + "] may not be null");
		}
		if (elements.isEmpty()) {
			throw new IllegalArgumentException("Collection in argument [" + name + "] may not be empty");
		}
	}

	public static void assertArgumentNotEmpty(Object[] elements, String name) {
		if (elements == null) {
			throw new IllegalArgumentException("Array in argument [" + name + "] may not be null");
		}
		if (elements.length == 0) {
			throw new IllegalArgumentException("Array in argument [" + name + "] may not be empty");
		}
	}

	public static void assertArgumentPositive(int value, String name) {
		if (value <= 0) {
			throw new IllegalArgumentException("Value of argument [" + name + "] may not be zero or negative");
		}
	}

	public static void assertArgumentZeroOrPositive(int value, String name) {
		if (value < 0) {
			throw new IllegalArgumentException("Value of argument [" + name + "] may not be negative");
		}
	}

	public static void assertArgumentElementsNotNull(Collection<?> elements, String name) {
		for (final Object element : elements) {
			if (element == null) {
				throw new IllegalArgumentException("Collection in argument [" + name + "] may not contain null elements");
			}
		}
	}

	public static void assertArgumentAndElementsNotNull(Collection<?> elements, String name) {
		assertArgumentNotNull(elements, name);
		assertArgumentElementsNotNull(elements, name);
	}

	public static void assertArgumentAndElementsNotNull(Object[] elements, String name) {
		assertArgumentNotNull(elements, name);
		assertArgumentElementsNotNull(Arrays.asList(elements), name);
	}
}
