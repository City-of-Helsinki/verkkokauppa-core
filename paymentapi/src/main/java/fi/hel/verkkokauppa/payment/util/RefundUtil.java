package fi.hel.verkkokauppa.payment.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;


public class RefundUtil {

    public static  String generateRefundPaymentId(String refundId) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String currentMinute = sdf.format(timestamp);

        return refundId + "_at_" + currentMinute;
    }

}
