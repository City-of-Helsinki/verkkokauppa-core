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
    private PaymentMethodListService paymentMethodListService;

    public PaymentMethodDto[] getFilteredPaymentMethodList(GetPaymentMethodListRequest request) {
        return paymentMethodListService.filterPaymentMethodList(
                request,
                paymentMethodListService.getOfflinePaymentMethodList(request.getCurrency())
        );
    }
}
