package fi.hel.verkkokauppa.payment.util;

import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;

import java.math.BigDecimal;
import java.math.BigInteger;

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
        return IdGeneratorUtil.generateIdWithTimestamp(orderId);
    }

    public static String parseMerchantId(OrderWrapper order) {
        if (
            order != null &&
            order.getItems() != null &&
            order.getItems().size() > 0 &&
            order.getItems().get(0).getMerchantId() != null
        ) {
            return order.getItems().get(0).getMerchantId();
        } else {
            return null;
        }
    }
}
