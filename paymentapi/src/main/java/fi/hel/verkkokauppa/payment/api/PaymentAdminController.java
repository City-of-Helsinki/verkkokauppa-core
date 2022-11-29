package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.payment.api.data.ChargeCardTokenRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.logic.fetcher.CancelPaymentFetcher;
import fi.hel.verkkokauppa.payment.logic.validation.PaymentReturnValidator;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentMethodService;
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

import java.util.List;

@RestController
public class PaymentAdminController {

    private Logger log = LoggerFactory.getLogger(PaymentAdminController.class);

    private final OnlinePaymentService onlinePaymentService;
    private final PaymentMethodService paymentMethodService;
    private final CancelPaymentFetcher cancelPaymentFetcher;
    private final PaymentReturnValidator paymentReturnValidator;
    private final SaveHistoryService saveHistoryService;

    @Autowired
    public PaymentAdminController(OnlinePaymentService onlinePaymentService,
                                  PaymentMethodService paymentMethodService,
                                  CancelPaymentFetcher cancelPaymentFetcher,
                                  PaymentReturnValidator paymentReturnValidator,
                                  SaveHistoryService saveHistoryService) {
        this.onlinePaymentService = onlinePaymentService;
        this.paymentMethodService = paymentMethodService;
        this.cancelPaymentFetcher = cancelPaymentFetcher;
        this.paymentReturnValidator = paymentReturnValidator;
        this.saveHistoryService = saveHistoryService;
    }

    @Autowired
    private PaymentFilterService filterService;

    @GetMapping(value = "/payment-admin/online/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Payment> getPayment(@RequestParam(value = "orderId") String orderId) {
        try {
            Payment payment = onlinePaymentService.getPaymentForOrder(orderId);
            if (payment == null) {
                throw new Exception("No payments found with given orderId. payment == null");
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
                        ChargeCardTokenResponse chargeCardTokenResponse = onlinePaymentService.chargeCardToken(request,payment);

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
}
