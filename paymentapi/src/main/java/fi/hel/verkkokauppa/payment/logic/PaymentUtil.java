package fi.hel.verkkokauppa.payment.logic;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class PaymentUtil {


    public static BigDecimal convertToCents(BigDecimal input) {
        BigDecimal multiplier = BigDecimal.valueOf(100L);
        return input.multiply(multiplier);
    }

    public static  String generatePaymentOrderNumber(String orderId) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String currentMinute = sdf.format(timestamp);

        return orderId + "_at_" + currentMinute;
    }

}
