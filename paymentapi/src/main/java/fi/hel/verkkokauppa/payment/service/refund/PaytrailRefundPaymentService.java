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
import fi.hel.verkkokauppa.payment.api.data.PaymentDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.model.refund.RefundGateway;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.model.refund.RefundPaymentStatus;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailRefundClient;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.repository.refund.RefundPaymentRepository;
import fi.hel.verkkokauppa.payment.util.RefundUtil;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.model.refunds.PaytrailRefundResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class PaytrailRefundPaymentService {

    @Autowired
    private RefundPaymentRepository refundPaymentRepository;

    @Autowired
    private SendEventService sendEventService;

    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private PaytrailPaymentContextBuilder paytrailPaymentContextBuilder;

    @Autowired
    private PaytrailRefundClient paytrailRefundClient;


    public void isValidUserToCreateRefundPayment(String refundId, String userId) {
        if (userId == null || userId.isEmpty()) {
            log.warn("creating refund payment without user rejected, refundId: " + refundId);
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("rejected-creating-payment-for-order-without-user", "rejected creating refund payment for refund without user, refundId id [" + refundId + "]")
            );
        }
    }

    public void isValidRefundStatusToCreateRefundPayment(String refundId, String refundStatus) {
        // check refund status, can only create refund payment for confirmed refunds
        if (!"confirmed".equals(refundStatus)) {
            log.warn("creating refund payment for unconfirmed refund rejected, refundId: " + refundId);
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("rejected-creating-refund-payment-for-unconfirmed-refund", "rejected creating refund payment for unconfirmed refund, refundId id [" + refundId + "]")
            );
        }
    }

    public void setRefundPaymentStatus(String paymentId, String status) {
        RefundPayment refundPayment = getRefundPayment(paymentId);
        refundPayment.setStatus(status);
        refundPaymentRepository.save(refundPayment);
    }

    public RefundPayment getRefundPaymentForOrder(String orderId) {
        List<RefundPayment> refundPayments = refundPaymentRepository.findByOrderId(orderId);

        RefundPayment paidRefundPayment = selectPaidRefundPayment(refundPayments);
        RefundPayment payablePayment = selectPayableRefundPayment(refundPayments);

        if (paidRefundPayment != null) {
            return paidRefundPayment;
        } else if (payablePayment != null) {
            return payablePayment;
        } else {
            return null;
        }
    }

    public List<RefundPayment> getPaymentsForOrder(String orderId, String namepace) {
        return refundPaymentRepository.findByNamespaceAndOrderId(namepace, orderId);
    }

    public List<RefundPayment> getPaymentsWithNamespaceAndOrderIdAndStatus(String orderId, String namepace, String status) {
        return refundPaymentRepository.findByNamespaceAndOrderIdAndStatus(namepace, orderId, status);
    }


    private RefundPayment selectPayableRefundPayment(List<RefundPayment> payments) {
        RefundPayment payablePayment = null;
        for (RefundPayment payment : payments) {
            // in an unpayable state
            if (Objects.equals(payment.getStatus(), RefundPaymentStatus.PAID_ONLINE) || Objects.equals(payment.getStatus(), RefundPaymentStatus.CANCELLED)) {
                continue;
            }

            // an earlier selected refund payment is newer
            if (payablePayment != null && payablePayment.getTimestamp().compareTo(payment.getTimestamp()) > 0) {
                continue;
            }

            payablePayment = payment;
        }

        return payablePayment;
    }

    private RefundPayment selectPaidRefundPayment(List<RefundPayment> payments) {
        for (RefundPayment payment : payments) {
            if (Objects.equals(payment.getStatus(), RefundPaymentStatus.PAID_ONLINE)) {
                return payment;
            }
        }
        return null;
    }

    public RefundPayment getRefundPayment(String refundId) {
        return refundPaymentRepository.findById(refundId).orElseThrow(() -> {
                    log.debug("refund not found, refundId: " + refundId);
                    return new CommonApiException(
                            HttpStatus.NOT_FOUND,
                            new Error("refund-not-found-from-backend", "refund with refund id [" + refundId + "] not found from backend")
                    );
                }
        );

    }

    public RefundPayment createRefundToPaytrailAndCreateRefundPayment(RefundRequestDataDto dto) {
        RefundDto refundDto = dto.getRefund().getRefund();
        String refundId = refundDto.getRefundId();

        isValidRefundStatusToCreateRefundPayment(
                refundId,
                refundDto.getStatus()
        );

        isValidUserToCreateRefundPayment(
                refundId,
                refundDto.getUser()
        );
        String orderType = getOrderType(dto.getOrder().getOrder());

        PaymentDto paymentDto = dto.getPayment();
        // Get merchantId from first refund item
        RefundItemDto refundItemDto = dto.getRefund().getItems().get(0);
        PaytrailPaymentContext context = createPaytrailRefundContext(refundDto, refundItemDto, paymentDto);

        String paymentTransactionId = paymentDto.getPaytrailTransactionId();
        String refundPaymentId = RefundUtil.generateRefundPaymentId(refundId);

        PaytrailRefundResponse refundResponse = paytrailRefundClient.createRefund(
                context,
                refundPaymentId,
                paymentTransactionId,
                refundDto
        );

        RefundPayment refundPayment = createRefundPayment(
                dto,
                paymentDto,
                orderType,
                refundResponse,
                refundPaymentId
        );

        if (refundPayment.getRefundPaymentId() == null || refundPayment.getRefundTransactionId() == null) {
            throw new RuntimeException("Didn't manage to create refund payment.");
        }

        return refundPayment;
    }

    private String getOrderType(OrderDto order) {
        boolean isRecurringOrder = order.getType().equals(OrderType.SUBSCRIPTION);
        return isRecurringOrder ? OrderType.SUBSCRIPTION : OrderType.ORDER;
    }

    public PaytrailPaymentContext createPaytrailRefundContext(RefundDto refundDto, RefundItemDto refundItemDto, PaymentDto paymentDto) {
        return paytrailPaymentContextBuilder.buildFor(
                refundDto.getNamespace(),
                refundItemDto.getMerchantId(),
                paymentDto.isShopInShopPayment()
        );
    }


    private RefundPayment createRefundPayment(
            RefundRequestDataDto dto,
            PaymentDto paymentDto,
            String orderType,
            PaytrailRefundResponse refundResponse,
            String refundId
    ) {
        RefundDto refundDto = dto.getRefund().getRefund();

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");

        String namespace = refundDto.getNamespace();

        RefundPayment refundPayment = new RefundPayment();

        refundPayment.setRefundPaymentId(refundId);
        refundPayment.setNamespace(refundDto.getNamespace());
        refundPayment.setOrderId(refundDto.getOrderId());
        refundPayment.setUserId(refundDto.getUser());
        refundPayment.setRefundMethod(paymentDto.getPaymentMethod());
        refundPayment.setRefundGateway(RefundGateway.PAYTRAIL.toString());
        refundPayment.setTimestamp(sdf.format(timestamp));
        refundPayment.setRefundType(orderType);

        refundPayment.setTotalExclTax(new BigDecimal(refundDto.getPriceNet()));
        refundPayment.setTaxAmount(new BigDecimal(refundDto.getPriceVat()));
        refundPayment.setTotal(new BigDecimal(refundDto.getPriceTotal()));
        refundPayment.setRefundId(refundDto.getRefundId());

        refundPayment.setRefundTransactionId(refundResponse.getTransactionId());

        refundPaymentRepository.save(refundPayment);
        log.debug("created refundPayment for namespace: " + namespace + " with refundId: " + refundId);

        return refundPayment;
    }

    public void triggerRefundPaymentPaidEvent(RefundPayment payment) {
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

    public void triggerRefundPaymentFailedEvent(Payment payment) {
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

    public RefundPayment findByIdValidateByUser(String namespace, String orderId, String userId) {
        RefundPayment refundPayment = getRefundPaymentForOrder(orderId);
        if (refundPayment == null) {
            log.debug("no returnable refundPayment, orderId: " + orderId);
            Error error = new Error("refund-payment-not-found-from-backend", "paid or payable refundPayment with order id [" + orderId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        String paymentUserId = refundPayment.getUserId();
        if (!paymentUserId.equals(userId)) {
            log.error("unauthorized attempt to load refundPayment, userId does not match");
            Error error = new Error("refundPayment-not-found-from-backend", "refundPayment with order id [" + orderId + "] and user id [" + userId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        return refundPayment;
    }

    public void updateRefundPaymentStatus(String refundId, PaymentReturnDto paymentReturnDto) {
        RefundPayment refundPayment = getRefundPayment(refundId);

        if (paymentReturnDto.isValid()) {
            if (paymentReturnDto.isPaymentPaid()) {
                // if not already paid earlier
                if (!PaymentStatus.PAID_ONLINE.equals(refundPayment.getStatus())) {
//                    setRefundPaymentStatus(refundId, PaymentStatus.PAID_ONLINE);
//                    triggerRefundPaymentPaidEvent(refundPayment);
                } else {
                    log.debug("not triggering events, refundPayment paid earlier, refundId: " + refundId);
                }
            } else if (!paymentReturnDto.isPaymentPaid() && !paymentReturnDto.isCanRetry()) {
                // if not already cancelled earlier
                if (!PaymentStatus.CANCELLED.equals(refundPayment.getStatus())) {
//                    setRefundPaymentStatus(refundId, PaymentStatus.CANCELLED);
//                    triggerRefundPaymentFailedEvent(refundPayment);
                } else {
                    log.debug("not triggering events, refund refundPayment cancelled earlier, refundId: " + refundId);
                }
            } else {
                log.debug("not triggering events, refund refundPayment not paid but can be retried, refundId: " + refundId);
            }
        }
    }

}
