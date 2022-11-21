package fi.hel.verkkokauppa.payment.service.refund;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.model.refund.RefundPaymentStatus;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.repository.refund.RefundPaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class RefundPaymentService {

    @Autowired
    private RefundPaymentRepository refundPaymentRepository;

    @Autowired
    private SendEventService sendEventService;

    @Autowired
    private SendNotificationService sendNotificationService;


    @Autowired
    private PaytrailPaymentContextBuilder paytrailPaymentContextBuilder;

    private void isValidUserToCreateRefundPayment(String refundId, String userId) {
        if (userId == null || userId.isEmpty()) {
            log.warn("creating refund payment without user rejected, refundId: " + refundId);
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("rejected-creating-payment-for-order-without-user", "rejected creating refund payment for refund without user, refundId id [" + refundId + "]")
            );
        }
    }

    private void isValidRefundStatusToCreatePayment(String refundId, String refundStatus) {
        // check refund status, can only create refund payment for confirmed refunds
        if (!"confirmed".equals(refundStatus)) {
            log.warn("creating refund payment for unconfirmed order rejected, refundId: " + refundId);
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("rejected-creating-refund-payment-for-unconfirmed-refund", "rejected creating refund payment for unconfirmed refund, refundId id [" + refundId + "]")
            );
        }
    }

    public void setPaymentStatus(String paymentId, String status) {
        RefundPayment payment = getRefund(paymentId);
        payment.setStatus(status);
        refundPaymentRepository.save(payment);
    }

    public RefundPayment getRefundPaymentForOrder(String orderId) {
        List<RefundPayment> payments = refundPaymentRepository.findByOrderId(orderId);

        RefundPayment paidPayment = selectPaidPayment(payments);
        RefundPayment payablePayment = selectPayablePayment(payments);

        if (paidPayment != null) {
            return paidPayment;
        } else if (payablePayment != null) {
            return payablePayment;
        } else {
            return null;
        }
    }

    public List<RefundPayment> getPaymentsForOrder(String orderId, String namepace) {
        return refundPaymentRepository.findByNamespaceAndOrderId(namepace,orderId);
    }

    public List<RefundPayment> getPaymentsWithNamespaceAndOrderIdAndStatus(String orderId, String namepace, String status) {
        return refundPaymentRepository.findByNamespaceAndOrderIdAndStatus(namepace, orderId, status);
    }

    // payment precedence selection from KYV-186
    private RefundPayment selectPayablePayment(List<RefundPayment> payments) {
        RefundPayment payablePayment = null;

        if (payments != null)
            for (RefundPayment payment : payments) {
                // in an unpayable state
                if (Objects.equals(payment.getStatus(), RefundPaymentStatus.PAID_ONLINE) || Objects.equals(payment.getStatus(), RefundPaymentStatus.CANCELLED)) {
                    continue;
                }

                // an earlier selected payment is newer
                if (payablePayment != null && payablePayment.getTimestamp().compareTo(payment.getTimestamp()) > 0) {
                    continue;
                }

                payablePayment = payment;
            }

        return payablePayment;
    }

    private RefundPayment selectPaidPayment(List<RefundPayment> payments) {
        if (payments != null) {
            for (RefundPayment payment : payments) {
                if (Objects.equals(payment.getStatus(), RefundPaymentStatus.PAID_ONLINE)) {
                    return payment;
                }
            }
        }

        return null;
    }

    public RefundPayment getRefund(String refundId) {
        Optional<RefundPayment> payment = refundPaymentRepository.findById(refundId);

        if (!payment.isPresent()) {
            log.debug("refund not found, refundId: " + refundId);
            Error error = new Error("refund-not-found-from-backend", "refund with refund id [" + refundId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        return payment.get();
    }
    // TODO RENAME AND CHECK
    public RefundPayment getPaymentRequestData(RefundRequestDataDto dto) {
        OrderDto order = dto.getOrder().getOrder();
        RefundDto refundDto = dto.getRefundAggregateDto().getRefund();
        RefundItemDto refundItemDto = dto.getRefundAggregateDto().getItems().get(0);
        String namespace = refundDto.getNamespace();
        String refundId = refundDto.getRefundId();
        String orderStatus = refundDto.getStatus();
        String userId = refundDto.getUser();

        isValidRefundStatusToCreatePayment(refundId, orderStatus);

        isValidUserToCreateRefundPayment(refundId, userId);

        // TODO
        boolean isRecurringOrder = order.getType().equals(OrderType.SUBSCRIPTION);
        String paymentType = isRecurringOrder ? OrderType.SUBSCRIPTION : OrderType.ORDER;
        // TODO

        // TODO TEST merchantId to refundItems!
        PaytrailPaymentContext context = paytrailPaymentContextBuilder.buildFor(namespace, refundItemDto.getMerchantId(), false);

//
//        ChargeRequest.PaymentTokenPayload tokenRequestPayload = paymentTokenPayloadBuilder.buildFor(dto, context);
//        log.debug("tokenRequestPayload: " + tokenRequestPayload);
//
//        String paymentId = tokenRequestPayload.getOrderNumber();
//        String token = paymentTokenFetcher.getToken(tokenRequestPayload);

        String paytrailTransactionId = ""; // TODO
        String paymentId = ""; // TODO

        RefundPayment refundPayment = createRefundPayment(dto, paymentType, paytrailTransactionId, paymentId);
        if (refundPayment.getRefundPaymentId() == null) {
            throw new RuntimeException("Didn't manage to create refund payment.");
        }

        return refundPayment;
    }


    private RefundPayment createRefundPayment(RefundRequestDataDto dto, String type, String paytrailTransactionId, String refundId) {
        OrderDto order = dto.getOrder().getOrder();
        RefundDto refundDto = dto.getRefundAggregateDto().getRefund();

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");

        String namespace = refundDto.getNamespace();

        RefundPayment refundPayment = new RefundPayment();
        // TODO CHECK GENERATION
        refundPayment.setRefundPaymentId(refundId);
        refundPayment.setNamespace(refundDto.getNamespace());
        refundPayment.setOrderId(refundDto.getOrderId());
        refundPayment.setUserId(refundDto.getUser());
        refundPayment.setRefundMethod(dto.getPaymentMethod());
        refundPayment.setTimestamp(sdf.format(timestamp));
        refundPayment.setRefundType(type);

        refundPayment.setPaytrailTransactionId(paytrailTransactionId);
        refundPayment.setTotalExclTax(new BigDecimal(refundDto.getPriceNet()));
        refundPayment.setTaxAmount(new BigDecimal(refundDto.getPriceVat()));
        refundPayment.setTotal(new BigDecimal(refundDto.getPriceTotal()));

        refundPaymentRepository.save(refundPayment);
        log.debug("created refundPayment for namespace: " + namespace + " with refundId: " + refundId);

        return refundPayment;
    }

    public void triggerPaymentPaidEvent(RefundPayment payment) {
        String now = DateTimeUtil.getDateTime();

        PaymentMessage.PaymentMessageBuilder paymentMessageBuilder = PaymentMessage.builder()
                .eventType(EventType.PAYMENT_PAID)
                .eventTimestamp(now)
                .namespace(payment.getNamespace())
                .paymentId(payment.getRefundPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .paymentPaidTimestamp(now)
                .orderType(payment.getRefundType());

        PaymentMessage paymentMessage = paymentMessageBuilder.build();

        orderPaidWebHookAction(paymentMessage);

        sendEventService.sendEventMessage(TopicName.PAYMENTS, paymentMessage);
        log.debug("triggered event REFUND_PAID for refundId: " + payment.getRefundPaymentId());
    }

    protected void orderPaidWebHookAction(PaymentMessage message) {
        try {
            sendNotificationService.sendPaymentMessageNotification(message);
        } catch (Exception e) {
            log.error("webhookAction: failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    public void triggerPaymentFailedEvent(Payment payment) {
        PaymentMessage paymentMessage = PaymentMessage.builder()
                .eventType(EventType.PAYMENT_FAILED)
                .namespace(payment.getNamespace())
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                //.paymentPaidTimestamp(payment.getTimestamp())
                .orderType(payment.getPaymentType())
                .build();
        sendEventService.sendEventMessage(TopicName.PAYMENTS, paymentMessage);
        log.debug("triggered event PAYMENT_FAILED for paymentId: " + payment.getPaymentId());
    }

    public RefundPayment findByIdValidateByUser(String namespace, String refundId, String userId) {
        RefundPayment refundPayment = getRefundPaymentForOrder(refundId);
        if (refundPayment == null) {
            log.debug("no returnable refundPayment, refundId: " + refundId);
            Error error = new Error("refund-payment-not-found-from-backend", "paid or payable refundPayment with order id [" + refundId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        String paymentUserId = refundPayment.getUserId();
        if (!paymentUserId.equals(userId)) {
            log.error("unauthorized attempt to load refundPayment, userId does not match");
            Error error = new Error("refundPayment-not-found-from-backend", "refundPayment with order id [" + refundId + "] and user id ["+ userId +"] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        return refundPayment;
    }

    public void updatePaymentStatus(String refundId, PaymentReturnDto paymentReturnDto) {
        RefundPayment payment = getRefund(refundId);

        if (paymentReturnDto.isValid()) {
            if (paymentReturnDto.isPaymentPaid()) {
                // if not already paid earlier
                if (!PaymentStatus.PAID_ONLINE.equals(payment.getStatus())) {
//                    setPaymentStatus(refundId, PaymentStatus.PAID_ONLINE);
//                    triggerPaymentPaidEvent(payment);
                } else {
                    log.debug("not triggering events, payment paid earlier, refundId: " + refundId);
                }
            } else if (!paymentReturnDto.isPaymentPaid() && !paymentReturnDto.isCanRetry()) {
                // if not already cancelled earlier
                if (!PaymentStatus.CANCELLED.equals(payment.getStatus())) {
//                    setPaymentStatus(refundId, PaymentStatus.CANCELLED);
//                    triggerPaymentFailedEvent(payment);
                } else {
                    log.debug("not triggering events, refund payment cancelled earlier, refundId: " + refundId);
                }
            } else {
                log.debug("not triggering events, refund payment not paid but can be retried, refundId: " + refundId);
            }
        }
    }

}
