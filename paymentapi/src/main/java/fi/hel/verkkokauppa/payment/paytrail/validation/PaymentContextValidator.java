package fi.hel.verkkokauppa.payment.paytrail.validation;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.http.HttpStatus;

public class PaymentContextValidator {

    public static void validateContext(PaytrailPaymentContext context) {
        if (context.isUseShopInShop() && StringUtils.isEmpty(context.getShopId())) {
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error(
                            "validation-failed-for-paytrail-payment-context-without-merchant-shop-id",
                            "Failed to validate paytrail payment context, merchant shop id not found for merchant [" + context.getInternalMerchantId() + "]"
                    )
            );
        }
        if (!context.isUseShopInShop() && StringUtils.isEmpty(context.getPaytrailMerchantId()) || StringUtils.isEmpty(context.getPaytrailSecretKey()) ) {
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error(
                            "validation-failed-for-paytrail-payment-context-without-paytrail-merchant-credentials",
                            "Failed to validate paytrail payment context, merchant credentials (merchant ID or secret key) are missing for merchant [" + context.getInternalMerchantId() + "]"
                    )
            );
        }
    }
}
