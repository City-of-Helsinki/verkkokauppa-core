package fi.hel.verkkokauppa.order.test.utils.payment;


import java.util.Map;
import java.util.stream.Collectors;

public class PaytrailHelper {

    public static Map<String, String> getFormParams(TestPaytrailPaymentResponse response, String providerId) {
        if (response.getProviders() == null) return null;

        return response.getProviders().stream()
                .filter(provider -> providerId.equals(provider.getId()))
                .findFirst()
                .map(provider -> provider.getParameters().stream()
                        .collect(Collectors.toMap(
                                TestPaytrailPaymentResponse.Parameter::getName,
                                TestPaytrailPaymentResponse.Parameter::getValue)))
                .orElse(null);
    }

    public static String getFormAction(TestPaytrailPaymentResponse response, String providerId) {
        return response.getProviders().stream()
                .filter(p -> providerId.equals(p.getId()))
                .map(TestPaytrailPaymentResponse.Provider::getUrl)
                .findFirst()
                .orElse(null);
    }
}
