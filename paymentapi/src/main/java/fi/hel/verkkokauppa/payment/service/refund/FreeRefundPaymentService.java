package fi.hel.verkkokauppa.payment.service.refund;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.message.RefundMessage;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundReturnDto;
import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.refund.RefundGateway;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.model.refund.RefundPaymentStatus;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailRefundClient;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.repository.refund.RefundPaymentRepository;
import fi.hel.verkkokauppa.payment.util.RefundUtil;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.constants.RefundStatus;
import org.helsinki.paytrail.model.refunds.PaytrailRefundResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class FreeRefundPaymentService {

    @Autowired
    private RefundPaymentRepository refundPaymentRepository;

    @Autowired
    private SendEventService sendEventService;

    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private PaytrailPaymentContextBuilder paytrailPaymentContextBuilder;

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

    public void setRefundPaymentStatus(String refundId, String status) {
        RefundPayment refundPayment = getRefundPaymentWithRefundId(refundId);
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

    public List<RefundPayment> getRefundPaymentForOrderByRefundId(String refundId) {
        return refundPaymentRepository.findByRefundId(refundId);
    }
    public RefundPayment getRefundPaymentByRefundPaymentId(String refundPaymentId) {
        return refundPaymentRepository.findById(refundPaymentId).orElse(null);
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

    public RefundPayment getRefundPaymentWithRefundId(String refundId) {
        return refundPaymentRepository.findById(refundId).orElseThrow(() -> {
                    log.debug("refund not found, refundId: " + refundId);
                    return new CommonApiException(
                            HttpStatus.NOT_FOUND,
                            new Error("refund-not-found-from-backend", "refund with refund id [" + refundId + "] not found from backend")
                    );
                }
        );

    }

    public RefundPayment createFreeRefundPayment(RefundRequestDataDto dto) throws CommonApiException {
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

        String refundPaymentId = RefundUtil.generateRefundPaymentId(refundId);

        RefundPayment refundPayment = createRefundPayment(
                dto,
                paymentDto,
                orderType,
                refundPaymentId
        );

        if (refundPayment.getRefundPaymentId() == null) {
            throw new RuntimeException("Didn't manage to create refund payment.");
        }

        refundPayment = refundPaymentRepository.save(refundPayment);

        return refundPayment;
    }

    private String getOrderType(OrderDto order) {
        boolean isRecurringOrder = order.getType().equals(OrderType.SUBSCRIPTION);
        return isRecurringOrder ? OrderType.SUBSCRIPTION : OrderType.ORDER;
    }


    private RefundPayment createRefundPayment(
            RefundRequestDataDto dto,
            PaymentDto paymentDto,
            String orderType,
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
        refundPayment.setRefundGateway(RefundGateway.FREE.toString());
        refundPayment.setTimestamp(sdf.format(timestamp));

        refundPayment.setTotalExclTax(new BigDecimal(refundDto.getPriceNet()));
        refundPayment.setTaxAmount(new BigDecimal(refundDto.getPriceVat()));
        refundPayment.setTotal(new BigDecimal(refundDto.getPriceTotal()));
        refundPayment.setRefundId(refundDto.getRefundId());

        refundPaymentRepository.save(refundPayment);
        log.debug("created refundPayment for namespace: " + namespace + " with refundId: " + refundId);

        return refundPayment;
    }

    public void triggerRefundPaymentPaidEvent(RefundPayment refundPayment) {
        String now = DateTimeUtil.getDateTime();

        RefundMessage.RefundMessageBuilder refundMessageBuilder = RefundMessage.builder()
                .eventType(EventType.REFUND_PAID)
                .timestamp(now)
                .namespace(refundPayment.getNamespace())
                .refundId((refundPayment.getRefundId()))
                .refundPaymentId(refundPayment.getRefundPaymentId())
                .orderId(refundPayment.getOrderId())
                .eventTimestamp(DateTimeUtil.getDateTime())
                .userId(refundPayment.getUserId());

        RefundMessage refundMessage = refundMessageBuilder.build();

        refundWebHookAction(refundMessage);
        log.debug("triggered event REFUND_PAID for refundPaymentId: " + refundPayment.getRefundPaymentId());
    }

    protected void refundWebHookAction(RefundMessage message) {
        try {
            sendNotificationService.sendRefundMessageNotification(message);
        } catch (Exception e) {
            log.error("webhookAction: failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    public void triggerRefundPaymentFailedEvent(RefundPayment refundPayment) {
        String now = DateTimeUtil.getDateTime();

        RefundMessage.RefundMessageBuilder refundMessageBuilder = RefundMessage.builder()
                .eventType(EventType.REFUND_FAILED)
                .timestamp(now)
                .namespace(refundPayment.getNamespace())
                .refundPaymentId(refundPayment.getRefundPaymentId())
                .refundId(refundPayment.getRefundId())
                .orderId(refundPayment.getOrderId())
                .eventTimestamp(DateTimeUtil.getDateTime())
                .userId(refundPayment.getUserId());

        RefundMessage refundMessage = refundMessageBuilder.build();

        refundWebHookAction(refundMessage);
        log.debug("triggered event REFUND_FAILED for refundId: " + refundPayment.getRefundId());
    }


    public void updateRefundPaymentStatus(String refundId, RefundReturnDto refundReturnDto) {
        RefundPayment refundPayment = getRefundPaymentWithRefundId(refundId);

        if (refundReturnDto.isValid()) {
            if (refundReturnDto.isRefundPaid()) {
                // if not already paid earlier
                if (!RefundPaymentStatus.PAID_ONLINE.equals(refundPayment.getStatus())) {
                    setRefundPaymentStatus(refundId, RefundPaymentStatus.PAID_ONLINE);
                    triggerRefundPaymentPaidEvent(refundPayment);
                } else {
                    log.debug("not triggering events, refundPayment paid earlier, refundId: " + refundId);
                }
            } else if (!refundReturnDto.isRefundPaid() && !refundReturnDto.isCanRetry()) {
                // if not already cancelled earlier
                if (!RefundPaymentStatus.CANCELLED.equals(refundPayment.getStatus())) {
                    setRefundPaymentStatus(refundId, RefundPaymentStatus.CANCELLED);
                    triggerRefundPaymentFailedEvent(refundPayment);
                } else {
                    log.debug("not triggering events, refund refundPayment cancelled earlier, refundId: " + refundId);
                }
            } else {
                log.debug("not triggering events, refund refundPayment not paid but can be retried, refundId: " + refundId);
            }
        }
    }

    public RefundReturnDto createRefundReturnDto(boolean isValid) {
        boolean isRefundPaid = true;
        boolean canRetry = false;

        return new RefundReturnDto(isValid, isRefundPaid, canRetry);
    }

    public boolean validatePayment(RefundPayment payment) {
        boolean isValid = false;
        if( payment != null ){
            BigDecimal zeroValue = BigDecimal.ZERO;
            boolean paymentValidation = payment.getTotal().compareTo(zeroValue) == 0;
            boolean gatewayValidation = payment.getRefundGateway().equals(RefundGateway.FREE.toString());
            // check that total is zero and gateway is free
            isValid = gatewayValidation && paymentValidation;
        }
        return isValid;
    }

}
