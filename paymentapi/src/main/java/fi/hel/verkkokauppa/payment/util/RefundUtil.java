package fi.hel.verkkokauppa.payment.util;

public class RefundUtil {

    public static  String generateRefundPaymentId(String refundId) {
        return IdGeneratorUtil.generateIdWithTimestamp(refundId);
    }

}
