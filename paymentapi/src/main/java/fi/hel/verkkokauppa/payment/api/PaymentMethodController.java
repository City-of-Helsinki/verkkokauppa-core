package fi.hel.verkkokauppa.payment.api;


import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.payment.service.PaymentMethodService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class PaymentMethodController {


    @Autowired
    private PaymentMethodService paymentMethodService;


    @PostMapping("/paymentmethod/order/setPaymentMethod")
    public ResponseEntity<OrderPaymentMethodDto> upsertOrderPaymentMethod(@RequestBody OrderPaymentMethodDto dto) {
        try {
            OrderPaymentMethodDto savedDto = paymentMethodService.upsertOrderPaymentMethod(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("order payment method setting failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-set-order-payment-method", "failed to set order payment method")
            );
        }

    }
}
