package fi.hel.verkkokauppa.payment.service;

import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.payment.api.OrderPaymentDto;

@Component
public class BillingPaymentService {
   
    public String createFromOrder(OrderPaymentDto dto) {
        //TODO a redirect url containing id of created Payment
        return null;
    }
     
}
