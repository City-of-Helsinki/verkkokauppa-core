package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.payment.api.data.*;
import fi.hel.verkkokauppa.payment.logic.fetcher.CancelPaymentFetcher;
import fi.hel.verkkokauppa.payment.logic.validation.PaymentReturnValidator;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentFilterService;
import fi.hel.verkkokauppa.payment.service.PaymentMethodService;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import org.helsinki.paytrail.model.payments.PaytrailPayment;
import org.helsinki.paytrail.model.payments.PaytrailPaymentMitChargeSuccessResponse;
import org.helsinki.vismapay.response.VismaPayResponse;
import org.helsinki.vismapay.response.payment.ChargeCardTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
public class PaymentAdminController {

    private Logger log = LoggerFactory.getLogger(PaymentAdminController.class);

    private final OnlinePaymentService onlinePaymentService;
    private final PaymentMethodService paymentMethodService;
    private final CancelPaymentFetcher cancelPaymentFetcher;
    private final PaymentReturnValidator paymentReturnValidator;
    private final SaveHistoryService saveHistoryService;
    private final PaymentPaytrailService paymentPaytrailService;
    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    public PaymentAdminController(OnlinePaymentService onlinePaymentService,
                                  PaymentMethodService paymentMethodService,
                                  CancelPaymentFetcher cancelPaymentFetcher,
                                  PaymentReturnValidator paymentReturnValidator,
                                  SaveHistoryService saveHistoryService,
                                  PaymentPaytrailService paymentPaytrailService) {
        this.onlinePaymentService = onlinePaymentService;
        this.paymentMethodService = paymentMethodService;
        this.cancelPaymentFetcher = cancelPaymentFetcher;
        this.paymentReturnValidator = paymentReturnValidator;
        this.saveHistoryService = saveHistoryService;
        this.paymentPaytrailService = paymentPaytrailService;
    }

    @Autowired
    private PaymentFilterService filterService;

    @GetMapping(value = "/payment-admin/online/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Payment> getPayment(@RequestParam(value = "orderId") String orderId) {
        try {
            Payment payment = onlinePaymentService.getPaymentForOrder(orderId);
            if (payment == null) {
                log.error("No payments found with given orderId. orderId: " + orderId);
                throw new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("failed-to-get-payment", "failed to get payment with order id [" + orderId + "]")
                );
            }
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
                payments = onlinePaymentService.getPaymentsWithNamespaceAndOrderIdAndStatus(orderId, namespace, paymentStatus);
            } else {
                payments = onlinePaymentService.getPaymentsForOrder(orderId, namespace);
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

    @PostMapping(value = "/payment-admin/paytrail/subscription-renewal-order-created-event", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> orderCreatedEventCallbackPaytrail(@RequestBody OrderMessage message) {
        log.debug("orderCreatedEventCallback OrderMessage: {}", message);
        try {
            log.debug("payment-api received ORDER_CREATED event for orderId: " + message.getOrderId());


            Payment existingPayment = onlinePaymentService.getPaymentForOrder(message.getOrderId());
            if (existingPayment != null && PaymentStatus.PAID_ONLINE.equals(existingPayment.getStatus())) {
                log.warn("paid payment exists, not creating new payment for orderId: " + message.getOrderId());
            } else {
                if ( message.getEndDate() != null && message.getEndDate().toLocalDate().isBefore(LocalDate.now(ZoneId.of("Europe/Helsinki"))) ){
                    // end date has passed. Do not throw error, just skip the payment so no retries will be made
                    log.info("Subscription " + message.getSubscriptionId() + " end date has passed. Not trying to renew payment.");
                }
                else if (Boolean.TRUE.equals(message.getIsSubscriptionRenewalOrder()) && message.isCardDefined()) {
                    Payment payment = onlinePaymentService.createSubscriptionRenewalPayment(message);
                    PaymentCardInfoDto card = new PaymentCardInfoDto(message.getCardToken(), message.getCardExpYear(), message.getCardExpMonth(), message.getCardLastFourDigits());
                    try {
                        PaytrailPaymentContext context = paymentPaytrailService.buildPaytrailContext(message.getNamespace(), message.getMerchantId());
                        PaymentReturnDto paymentReturnDto = new PaymentReturnDto(true, false, false, false);
                        String transactionId = null;
                        try {
                            PaytrailPaymentMitChargeSuccessResponse mitCharge = paymentPaytrailService.createMitCharge(context, payment.getPaymentId(), message);
                            paymentReturnDto = new PaymentReturnDto(true, true, false, false);
                            transactionId = mitCharge.getTransactionId();
                        } catch (Exception e) {
                            log.info("subscription renewal mit charge failed for orderId: " + payment.getOrderId(), e);
                            onlinePaymentService.updatePaymentStatus(payment.getPaymentId(), paymentReturnDto, card);
                            throw e;
                        }
                        onlinePaymentService.updatePaymentStatus(payment.getPaymentId(), paymentReturnDto, card);

                        // update payment from paytrail payment
                        try {
                            PaytrailPayment paytrailPayment = paymentPaytrailService.getPaytrailPayment(transactionId, message.getNamespace(), message.getMerchantId());
                            Payment updatedPayment = this.paymentPaytrailService.updatePaymentWithPaytrailPayment(payment.getPaymentId(), paytrailPayment);
                        } catch (CommonApiException cae) {
                            // do not fail renewal just because updating payment info from paytrail failed
                            log.error("/payment-admin/paytrail/subscription-renewal-order-created-event CommonApiException", cae);
                        } catch (Exception e) {
                            // do not fail renewal just because updating payment info from paytrail failed
                            log.error("/payment-admin/paytrail/subscription-renewal-order-created-event response failed", e);
                        }

                        if (transactionId != null) {
                            onlinePaymentService.setPaytrailTransactionId(payment.getPaymentId(), transactionId);
                            paymentPaytrailService.sendMitChargeNotify(payment.getOrderId(), payment.getPaymentId());
                        }
                    } catch (Exception e) {
                        log.info("subscription renewal payment failed for orderId: " + payment.getOrderId(), e);
                        log.error(
                                "Endpoint: /payment-admin/paytrail/subscription-renewal-order-created-event. Subscription renewal handling failed with exception for order: " + payment.getOrderId(),
                                e
                        );
                        throw e;
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

    @PostMapping(value = "/payment-admin/subscription-renewal-order-created-event", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> orderCreatedEventCallback(@RequestBody OrderMessage message) {
        // TODO remove later?
        log.debug("orderCreatedEventCallback OrderMessage: {}", message);
        try {
            log.debug("payment-api received ORDER_CREATED event for orderId: " + message.getOrderId());

            Payment existingPayment = onlinePaymentService.getPaymentForOrder(message.getOrderId());
            if (existingPayment != null && PaymentStatus.PAID_ONLINE.equals(existingPayment.getStatus())) {
                log.warn("paid payment exists, not creating new payment for orderId: " + message.getOrderId());
            } else {
                if (Boolean.TRUE.equals(message.getIsSubscriptionRenewalOrder()) && message.getCardToken() != null) {
                    Payment payment = onlinePaymentService.createSubscriptionRenewalPayment(message);

                    ChargeCardTokenRequestDataDto request = onlinePaymentService.createChargeCardTokenRequestDataDto(message, payment.getPaymentId());

                    try {
                        ChargeCardTokenResponse chargeCardTokenResponse = onlinePaymentService.chargeCardToken(request, payment);

                        PaymentReturnDto paymentReturnDto = paymentReturnValidator.validateReturnValues(
                                true,
                                chargeCardTokenResponse.getResult().toString(),
                                chargeCardTokenResponse.getSettled().toString()
                        );
                        onlinePaymentService.updatePaymentStatus(payment.getPaymentId(), paymentReturnDto);
                        // TODO remove later?
                        log.debug("paymentReturnDto {}", paymentReturnDto);
                    } catch (Exception e) {
                        log.debug("subscription renewal payment failed for orderId: " + payment.getOrderId(), e);
                        onlinePaymentService.updatePaymentStatus(payment.getPaymentId(), new PaymentReturnDto(true, false, false, false));
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

            Payment payment = onlinePaymentService.getPaymentRequestDataForCardRenewal(dto);
            String vismaPaymentUrl = onlinePaymentService.getPaymentUrl(payment);
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

    @PostMapping(value = "/payment-admin/online/save-payment-filters", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PaymentFilterDto>> savePaymentFilters(@RequestBody List<PaymentFilterDto> paymentFilters) {
        try {
            List<PaymentFilter> filterModelList = filterService.savePaymentFilters(paymentFilters);
            List<PaymentFilterDto> filterDtoList = filterService.mapPaymentFilterListToDtoList(filterModelList);
            return ResponseEntity.status(HttpStatus.CREATED).body(filterDtoList);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("saving payment filters failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-save-payment-filters", "failed to save payment filter(s)")
            );
        }
    }

    @GetMapping(value = "/payment-admin/get-payment-filters", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PaymentFilterDto>> getPaymentFilters(@RequestParam(value = "referenceType") String referenceType, @RequestParam(value = "referenceId") String referenceId) {
        try {
            List<PaymentFilter> paymentFilters = filterService.findPaymentFiltersByReferenceTypeAndReferenceId(referenceType, referenceId);
            return ResponseEntity.status(HttpStatus.OK).body(filterService.mapPaymentFilterListToDtoList(paymentFilters));
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting payment filters failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-payment-filters", "failed to get payment filter(s)")
            );
        }
    }

    @GetMapping(value = "/payment-admin/payment-method", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PaymentMethodDto>> getPaymentMethods() {
        List<PaymentMethodDto> paymentMethodDtos = paymentMethodService.getAllPaymentMethods();
        return ResponseEntity.status(HttpStatus.OK).body(paymentMethodDtos);
    }

    @GetMapping(value = "/payment-admin/payment-method/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentMethodDto> getPaymentMethodByCode(@PathVariable String code) {
        try {
            PaymentMethodDto paymentMethodDto = paymentMethodService.getPaymenMethodByCode(code);
            return ResponseEntity.status(HttpStatus.OK).body(paymentMethodDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-payment-method", "failed to get payment method")
            );
        }
    }

    @PostMapping(value = "/payment-admin/payment-method", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentMethodDto> createPaymentMethod(@RequestBody PaymentMethodDto dto) {
        try {
            PaymentMethodDto newPaymentMethodDto = paymentMethodService.createNewPaymentMethod(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(newPaymentMethodDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-payment-method", "failed to create a new payment method")
            );
        }
    }

    @PutMapping(value = "/payment-admin/payment-method/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentMethodDto> updatePaymentMethod(@PathVariable String code, @RequestBody PaymentMethodDto dto) {
        try {
            PaymentMethodDto updatedPaymentMethodDto = paymentMethodService.updatePaymentMethod(code, dto);
            return ResponseEntity.status(HttpStatus.OK).body(updatedPaymentMethodDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-update-payment-method", "failed to update payment method, code:" + code)
            );
        }
    }

    @DeleteMapping(value = "/payment-admin/payment-method/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deletePaymentMethod(@PathVariable String code) {
        try {
            paymentMethodService.deletePaymentMethod(code);
            return ResponseEntity.status(HttpStatus.OK).body(code);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-delete-payment-method", "failed to delete payment method, code:" + code)
            );
        }
    }

    @PutMapping(value = "/payment-admin/payment-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> setPaymentStatus(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "status") String status) {
        try {
            Payment payment = onlinePaymentService.getPaymentForOrder(orderId);
            if (payment == null) {
                log.error("No payments found with given orderId. orderId: " + orderId);
                throw new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("failed-to-get-payment", "failed to get payment with order id [" + orderId + "]")
                );
            }
            onlinePaymentService.setPaymentStatus(payment.getPaymentId(), status);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("setting payment status failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-set-payment-status", "failed to set payment status with order id [" + orderId + "]")
            );
        }
    }
}
