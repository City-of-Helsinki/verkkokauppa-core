package fi.hel.verkkokauppa.order.api.cron.search;

import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import fi.hel.verkkokauppa.order.api.cron.search.dto.PaymentResultDto;
import fi.hel.verkkokauppa.order.model.Order;
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
public class SearchUnAccountedPayments {
    @Autowired
    private SearchOrderService searchOrderService;

    @Autowired
    private SearchPaymentService searchPaymentService;

    @Autowired
    private SearchAccountingService searchAccountingService;


    public List<PaymentResultDto> findUnaccountedPayments() throws IOException {
        // Fetch unaccounted orders and log the size
        List<Order> unaccountedOrders = searchOrderService.getUnaccountedOrders();
        log.info("Unaccounted orders retrieved: {}", unaccountedOrders.size());

        List<String> orderIds = unaccountedOrders.stream().map(Order::getOrderId).collect(Collectors.toList());
        log.info("Order IDs extracted: {}", orderIds.size());

        if (orderIds.isEmpty()) {
            log.info("No unaccounted orders found. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch payments that match the status and order IDs, and log the size
        List<PaymentResultDto> matchedPayments = searchPaymentService.findPaymentsByStatusAndOrderIds(orderIds, "payment_paid_online");
        log.info("Matched payments with status 'payment_paid_online': {}", matchedPayments.size());

        if (matchedPayments.isEmpty()) {
            log.info("No matched payments found for the unaccounted orders. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch accounted order IDs and log the size
        Set<String> accountedOrderIds = searchAccountingService.getAccountedOrderIds(
                matchedPayments.stream().map(PaymentResultDto::getOrderId).collect(Collectors.toList())
        );
        log.info("Accounted order IDs retrieved: {}", accountedOrderIds.size());

        return getPaymentResultDtos(matchedPayments, accountedOrderIds);
    }

    private static List<PaymentResultDto> getPaymentResultDtos(List<PaymentResultDto> matchedPayments, Set<String> accountedOrderIds) {
        // Filter out unaccounted payments and log the result size
        List<PaymentResultDto> unaccountedPayments = matchedPayments.stream().filter(Objects::nonNull)
                .filter(payment ->
                        (payment.getPaymentGateway() != null && payment.getPaymentGateway().equals(PaymentGatewayEnum.INVOICE.toString()))
                                || !accountedOrderIds.contains(payment.getOrderId())
                ).collect(Collectors.toList());

        log.info("Unaccounted payments found: {}", unaccountedPayments.size());
        log.info("Unaccounted payment ids: {}", unaccountedPayments.stream().map(PaymentResultDto::getPaymentId));
        log.info("Unaccounted payment gateway null: {}", unaccountedPayments.stream().filter(Objects::nonNull).filter(paymentResultDto -> paymentResultDto.getPaymentGateway() == null).map(PaymentResultDto::getPaymentId));

        return unaccountedPayments;
    }

    public List<PaymentResultDto> findUnaccountedPayments(LocalDateTime createdAfter) throws IOException {
        // Fetch unaccounted orders and log the size
        List<Order> unaccountedOrders = searchOrderService.getUnaccountedOrders(createdAfter);
        log.info("Unaccounted orders retrieved: {}", unaccountedOrders.size());

        List<String> orderIds = unaccountedOrders.stream().map(Order::getOrderId).collect(Collectors.toList());
        log.info("Order IDs extracted: {}", orderIds.size());

        if (orderIds.isEmpty()) {
            log.info("No unaccounted orders found. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch payments that match the status and order IDs, and log the size
        List<PaymentResultDto> matchedPayments = searchPaymentService.findPaymentsByStatusAndOrderIds(orderIds, "payment_paid_online");
        log.info("Matched payments with status 'payment_paid_online': {}", matchedPayments.size());

        if (matchedPayments.isEmpty()) {
            log.info("No matched payments found for the unaccounted orders. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch accounted order IDs and log the size
        Set<String> accountedOrderIds = searchAccountingService.getAccountedOrderIds(
                matchedPayments.stream().map(PaymentResultDto::getOrderId).collect(Collectors.toList())
        );
        log.info("Accounted order IDs retrieved: {}", accountedOrderIds.size());

        // Filter out unaccounted payments and log the result size
        return getPaymentResultDtos(matchedPayments, accountedOrderIds);
    }
}
