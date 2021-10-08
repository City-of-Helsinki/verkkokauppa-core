package fi.hel.verkkokauppa.payment.api.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public enum PaymentMethodFilter {

    // If namespace has multiple filters, all available methods for all will be returned
    ORDER_TYPE(Collections.singletonList("asukaspysakointi")),

    ;

    List<String> namespaces;

    PaymentMethodFilter(List<String> namespaces) {
        this.namespaces = namespaces;
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public static List<PaymentMethodFilter> getByNamespace(String namespace) {
        return Arrays.stream(values())
                .filter(paymentMethodFilter -> paymentMethodFilter.getNamespaces().contains(namespace))
                .collect(Collectors.toList());
    }

}
