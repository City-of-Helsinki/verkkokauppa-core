package fi.hel.verkkokauppa.order.api.cron.search.refund;

import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import fi.hel.verkkokauppa.order.api.cron.search.dto.RefundResultDto;

import fi.hel.verkkokauppa.order.model.refund.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SearchUnAccountedRefunds {
    @Autowired
    private SearchRefundService searchRefundService;

    @Autowired
    private SearchRefundPaymentService searchRefundPaymentService;

    @Autowired
    private SearchRefundAccountingService searchRefundAccountingService;


    public List<RefundResultDto> findUnaccountedRefunds() throws IOException {
        // Fetch unaccounted orders and log the size
        List<Refund> unaccountedOrders = searchRefundService.getUnaccountedRefunds();
        log.info("Unaccounted orders retrieved: {}", unaccountedOrders.size());

        List<String> orderIds = unaccountedOrders.stream().map(Refund::getOrderId).collect(Collectors.toList());
        log.info("Order IDs extracted: {}", orderIds.size());

        if (orderIds.isEmpty()) {
            log.info("No unaccounted refunds found. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch payments that match the status and order IDs, and log the size
        List<RefundResultDto> matchedPayments = searchRefundPaymentService.findRefundsByStatusAndOrderIds(orderIds, "payment_paid_online");
        log.info("Matched payments with status 'payment_paid_online': {}", matchedPayments.size());

        if (matchedPayments.isEmpty()) {
            log.info("No matched payments found for the unaccounted orders. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch accounted order IDs and log the size
        Set<String> accountedOrderIds = searchRefundAccountingService.getAccountedOrderIds(
                matchedPayments.stream().map(RefundResultDto::getOrderId).collect(Collectors.toList())
        );
        log.info("Refund accounted order IDs retrieved: {}", accountedOrderIds.size());

        return getRefundResultDtos(matchedPayments, accountedOrderIds);
    }

    private static List<RefundResultDto> getRefundResultDtos(List<RefundResultDto> matchedPayments, Set<String> accountedOrderIds) {
        // Filter out unaccounted payments and log the result size
        List<RefundResultDto> unaccountedRefunds = matchedPayments.stream().filter(Objects::nonNull)
                .filter(payment ->
                        (payment.getRefundGateway() != null && payment.getRefundGateway().equals(PaymentGatewayEnum.INVOICE.toString()))
                                || !accountedOrderIds.contains(payment.getOrderId())
                ).collect(Collectors.toList());

        log.info("Unaccounted refunds found: {}", unaccountedRefunds.size());
        log.info("Unaccounted refund ids: {}", unaccountedRefunds.stream().map(RefundResultDto::getRefundId));
        log.info("Unaccounted refund gateway null: {}", unaccountedRefunds.stream().filter(Objects::nonNull).filter(paymentResultDto -> paymentResultDto.getRefundGateway() == null).map(RefundResultDto::getRefundId));

        return unaccountedRefunds;
    }

    public List<RefundResultDto> findUnaccountedRefunds(LocalDateTime createdAfter) throws IOException {
        // Fetch unaccounted refunds and log the size
        List<Refund> unaccountedRefunds = searchRefundService.getUnaccountedRefunds(createdAfter);
        log.info("Unaccounted refunds retrieved: {}", unaccountedRefunds.size());

        List<String> orderIds = unaccountedRefunds.stream().map(Refund::getOrderId).collect(Collectors.toList());
        log.info("Order IDs extracted: {}", orderIds.size());

        if (orderIds.isEmpty()) {
            log.info("No unaccounted refunds found. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch payments that match the status and order IDs, and log the size
        List<RefundResultDto> matchedPayments = searchRefundPaymentService.findRefundsByStatusAndOrderIds(orderIds, "refund_paid_online");
        log.info("Matched refunds with status 'refund_paid_online': {}", matchedPayments.size());

        if (matchedPayments.isEmpty()) {
            log.info("No matched refunds found for the unaccounted refunds. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch accounted order IDs and log the size
        Set<String> accountedOrderIds = searchRefundAccountingService.getAccountedOrderIds(
                matchedPayments.stream().map(RefundResultDto::getOrderId).collect(Collectors.toList())
        );
        log.info("Accounted refund order IDs retrieved: {}", accountedOrderIds.size());

        // Filter out unaccounted payments and log the result size
        return getRefundResultDtos(matchedPayments, accountedOrderIds);
    }
}
