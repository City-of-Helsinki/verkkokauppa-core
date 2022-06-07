package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.payment.api.data.ChargeCardTokenRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.logic.fetcher.CancelPaymentFetcher;
import fi.hel.verkkokauppa.payment.logic.validation.PaymentReturnValidator;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentFilterService;
import org.helsinki.vismapay.response.VismaPayResponse;
import org.helsinki.vismapay.response.payment.ChargeCardTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
public class PaymentAdminController {

    private Logger log = LoggerFactory.getLogger(PaymentAdminController.class);

    @Autowired
    private OnlinePaymentService service;
    @Autowired
    private CancelPaymentFetcher cancelPaymentFetcher;

    @Autowired
    private PaymentReturnValidator paymentReturnValidator;
    @Autowired
    private SaveHistoryService saveHistoryService;

    @Autowired
    private PaymentFilterService filterService;

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

    @GetMapping(value = "/payment-admin/online/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Payment>> getPayments(@RequestParam(value = "orderId") String orderId,
                                                     @RequestParam(value = "namespace") String namespace,
                                                     @RequestParam(value = "paymentStatus", required = false) String paymentStatus
    ) {
        try {
            List<Payment> payments;
            if (paymentStatus != null) {
                payments = service.getPaymentsWithNamespaceAndOrderIdAndStatus(orderId, namespace, paymentStatus);
            } else {
                payments = service.getPaymentsForOrder(orderId, namespace);
            }
            return ResponseEntity.status(HttpStatus.OK).body(payments);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting payments failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-payments", "failed to get payments with order id [" + orderId + "]")
            );
        }
    }

    @PostMapping(value = "/payment-admin/subscription-renewal-order-created-event", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> orderCreatedEventCallback(@RequestBody OrderMessage message) {
        // TODO remove later?
        log.debug("orderCreatedEventCallback OrderMessage: {}", message);
        try {
            log.debug("payment-api received ORDER_CREATED event for orderId: " + message.getOrderId());

            Payment existingPayment = service.getPaymentForOrder(message.getOrderId());
            if (existingPayment != null && PaymentStatus.PAID_ONLINE.equals(existingPayment.getStatus())) {
                log.warn("paid payment exists, not creating new payment for orderId: " + message.getOrderId());
            } else {
                if (Boolean.TRUE.equals(message.getIsSubscriptionRenewalOrder()) && message.getCardToken() != null) {
                    Payment payment = service.createSubscriptionRenewalPayment(message);

                    ChargeCardTokenRequestDataDto request = service.createChargeCardTokenRequestDataDto(message, payment.getPaymentId());

                    try {
                        ChargeCardTokenResponse chargeCardTokenResponse = service.chargeCardToken(request,payment);

                        PaymentReturnDto paymentReturnDto = paymentReturnValidator.validateReturnValues(
                                true,
                                chargeCardTokenResponse.getResult().toString(),
                                chargeCardTokenResponse.getSettled().toString()
                        );
                        service.updatePaymentStatus(payment.getPaymentId(), paymentReturnDto);
                        // TODO remove later?
                        log.debug("paymentReturnDto {}", paymentReturnDto);
                    } catch (Exception e) {
                        log.debug("subscription renewal payment failed for orderId: " + payment.getOrderId(), e);
                        service.updatePaymentStatus(payment.getPaymentId(), new PaymentReturnDto(true, false, false, false));
                    }
                } else {
                    log.warn("not a subscription renewal order, not creating new payment for orderId: " + message.getOrderId());
                }
            }
            saveHistoryService.saveOrderMessageHistory(message);
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

    @PostMapping(value = "/payment-admin/online/create/card-renewal-payment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createCardRenewalPayment(@RequestBody GetPaymentRequestDataDto dto) {
        try {

            Payment payment = service.getPaymentRequestDataForCardRenewal(dto);
            String vismaPaymentUrl = service.getPaymentUrl(payment);
            return ResponseEntity.status(HttpStatus.CREATED).body(vismaPaymentUrl);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("creating payment or card-renewal-payment failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-card-renewal-payment", "failed to create card renewal payment")
            );
        }
    }

    @GetMapping(value = "/payment-admin/online/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VismaPayResponse> cancelPayment(@RequestParam(value = "paymentId") String paymentId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    cancelPaymentFetcher.cancelPayment(paymentId)
            );
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("creating payment or card-renewal-payment failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-payment", "failed to create payment")
            );
        }
    }

    @PostMapping(value = "/payment-admin/online/save-payment-filter", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PaymentFilterDto>> savePaymentFilters(@RequestBody @Valid List<PaymentFilterDto> paymentFilters) {
        try {
            if (paymentFilters.isEmpty()) {
                throw new CommonApiException(
                        HttpStatus.BAD_REQUEST,
                        new Error("empty-payment-filter-list", "no payment filters given to save")
                );
            }
            List<PaymentFilter> filtersModel = filterService.savePaymentFilters(paymentFilters);
            List<PaymentFilterDto> filtersDto = filterService.mapPaymentFilterListToDtoList(filtersModel);
            return ResponseEntity.status(HttpStatus.CREATED).body(filtersDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("saving payment filters failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-save-payment-filter", "failed to save payment filter(s)")
            );
        }
    }

}
