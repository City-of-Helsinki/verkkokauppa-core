package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import org.springframework.stereotype.Service;

@Service
public class BillingPaymentService {
   
    public String createFromOrder(GetPaymentRequestDataDto dto) {
        //TODO a real redirect url
        return "https://localhost/?paymentId=123";
    }
}
