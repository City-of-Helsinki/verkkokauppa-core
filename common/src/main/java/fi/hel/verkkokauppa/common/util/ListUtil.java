package fi.hel.verkkokauppa.common.util;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;

public class ListUtil {
    public static <T> Optional<T> last(List<T> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    public static <T> T[] mergeArrays(Class<T> clazz, T[] firstArr, T[] secondArr) {
        int firstLen = firstArr.length;
        int secondLen = secondArr.length;
        T[] merged = (T[]) Array.newInstance(clazz, firstLen + secondLen);
        System.arraycopy(firstArr, 0, merged, 0, firstLen);
        System.arraycopy(secondArr, 0, merged, firstLen, secondLen);
        return merged;
    }
}
