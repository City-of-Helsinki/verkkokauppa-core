package fi.hel.verkkokauppa.payment.api.data;

import java.util.Arrays;
import java.util.List;

public enum PaymentMethodFilter {

    ORDER_TYPE

    ;

    public static List<PaymentMethodFilter> getAll() {
        return Arrays.asList(values());
    }

}
