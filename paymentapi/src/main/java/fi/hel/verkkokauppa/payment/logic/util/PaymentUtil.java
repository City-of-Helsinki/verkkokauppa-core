package fi.hel.verkkokauppa.payment.logic.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class PaymentUtil {


    public static BigDecimal convertToCents(BigDecimal input) {
        BigDecimal multiplier = BigDecimal.valueOf(100L);
        return input.multiply(multiplier);
    }

    /**
     * Creates correct amount to send for visma.
     * Ex: If you want to create 1 eur payment using visma you need to change price to 100
     * Returns 1 * 100 = 100 as big integer.
     */
    public static BigInteger eurosToBigInteger(Integer amount) {
        BigDecimal multiplier = BigDecimal.valueOf(100L);
        return (new BigDecimal(amount)).multiply(multiplier).toBigInteger();
    }

    public static  String generatePaymentOrderNumber(String orderId) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String currentMinute = sdf.format(timestamp);

        return orderId + "_at_" + currentMinute;
    }

}
