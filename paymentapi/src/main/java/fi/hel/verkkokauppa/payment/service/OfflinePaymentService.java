package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentMethodListRequest;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OfflinePaymentService {

    @Autowired
    private PaymentMethodService paymentMethodService;

    public PaymentMethodDto[] getFilteredPaymentMethodList(GetPaymentMethodListRequest request) {
        return paymentMethodService.filterPaymentMethodList(
                request,
                paymentMethodService.getOfflinePaymentMethodList(request.getCurrency())
        );
    }
}
