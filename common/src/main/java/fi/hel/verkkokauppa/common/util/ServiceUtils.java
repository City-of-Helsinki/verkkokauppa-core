package fi.hel.verkkokauppa.common.util;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class ServiceUtils {


    public static <T> Set<T> combineKeySets(Set<T> keySet1, Set<T> keySet2, Class<T> objectClass) {
        Set<T> combinedSet = new HashSet<>();

        if (keySet1 != null && !keySet1.isEmpty()) {
            combinedSet.addAll(keySet1);
        }
        if (keySet2 != null && !keySet2.isEmpty()) {
            combinedSet.addAll(keySet2);
        }

        return combinedSet;
    }
}
