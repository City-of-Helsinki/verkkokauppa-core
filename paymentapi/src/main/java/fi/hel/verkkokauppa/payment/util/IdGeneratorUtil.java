package fi.hel.verkkokauppa.payment.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class IdGeneratorUtil {

    public static  String generateIdWithTimestamp(String id) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String currentMinute = sdf.format(timestamp);

        return id + "_at_" + currentMinute;
    }

}
