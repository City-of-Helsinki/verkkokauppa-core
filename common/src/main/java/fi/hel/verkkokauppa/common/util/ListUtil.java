package fi.hel.verkkokauppa.common.util;

import java.util.List;
import java.util.Optional;

public class ListUtil {
    public static <T> Optional<T> last(List<T> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }
}
