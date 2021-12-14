package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.payment.api.data.ChargeCardTokenRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.logic.PaymentReturnValidator;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import org.helsinki.vismapay.response.payment.ChargeCardTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentAdminController {

    private Logger log = LoggerFactory.getLogger(PaymentAdminController.class);

    @Autowired
    private OnlinePaymentService service;

    @Autowired
    private PaymentReturnValidator paymentReturnValidator;


    @GetMapping(value = "/payment-admin/online/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Payment> getPayment(@RequestParam(value = "orderId") String orderId) {
        try {
            Payment payment = service.getPaymentForOrder(orderId);
            return ResponseEntity.status(HttpStatus.OK).body(payment);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting payment failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-payment", "failed to get payment with order id [" + orderId + "]")
            );
        }
    }

    @PostMapping(value = "/payment-admin/subscription-renewal-order-created-event", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> orderCreatedEventCallback(@RequestBody OrderMessage message) {

        try {
            log.debug("payment-api received ORDER_CREATED event for orderId: " + message.getOrderId());

            Payment existingPayment = service.getPaymentForOrder(message.getOrderId());
            if (existingPayment != null && PaymentStatus.PAID_ONLINE.equals(existingPayment.getStatus())) {
                log.warn("paid payment exists, not creating new payment for orderId: " + message.getOrderId());
            } else {
                if (Boolean.TRUE.equals(message.getIsSubscriptionRenewalOrder()) && message.getCardToken() != null) {
                    Payment payment = service.createSubscriptionRenewalPayment(message);

                    ChargeCardTokenRequestDataDto request = service.createChargeCardTokenRequestDataDto(message);

                    try {
                        ChargeCardTokenResponse chargeCardTokenResponse = service.chargeCardToken(request);

                        PaymentReturnDto paymentReturnDto = paymentReturnValidator.validateReturnValues(
                                true,
                                chargeCardTokenResponse.getResult().toString(),
                                chargeCardTokenResponse.getSettled().toString());
                        service.updatePaymentStatus(payment.getPaymentId(), paymentReturnDto);
                    } catch (Exception e) {
                        log.debug("subscription renewal payment failed for orderId: " + payment.getOrderId(), e);
                        service.updatePaymentStatus(payment.getPaymentId(), new PaymentReturnDto(true,false,false));
                    }
                } else {
                    log.warn("not a subscription renewal order, not creating new payment for orderId: " + message.getOrderId());
                }
            }

            return ResponseEntity.ok().build();

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("subscription renewal payment failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("subscription-renewal-payment-failed", "subscription renewal payment failed")
            );
        }
    }

}
