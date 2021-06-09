package fi.hel.verkkokauppa.common.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class IterableUtils {

	public static <T> List<T> iterableToList(final Iterable<? extends T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false)
						.collect(Collectors.toList());
	}

	public static boolean isEmpty(Collection collection) {
		return (collection == null || collection.isEmpty());
	}

	public static boolean isNotEmpty(Collection collection) {
		return (collection != null && !collection.isEmpty());
	}

	public static boolean isNotEmpty(Object[] array) {
		return (array != null && array.length > 0);
	}

	public static boolean isEmpty(Object[] array) {
		return (array == null || array.length == 0);
	}

	public static boolean isEmpty(Map map) {
		return (map == null || map.isEmpty());
	}

	public static boolean isNotEmpty(Map map) {
		return (map != null && !map.isEmpty());
	}
}
