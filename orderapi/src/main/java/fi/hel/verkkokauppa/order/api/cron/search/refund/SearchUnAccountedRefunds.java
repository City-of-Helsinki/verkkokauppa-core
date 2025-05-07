package fi.hel.verkkokauppa.order.api.cron.search.refund;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import fi.hel.verkkokauppa.common.payment.RefundPaymentCommonDto;
import fi.hel.verkkokauppa.common.payment.dto.UpdateFromPaytrailRefundDto;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.order.api.cron.search.dto.RefundResultDto;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import fi.hel.verkkokauppa.order.service.refund.RefundItemService;
import fi.hel.verkkokauppa.order.service.refund.RefundService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ServiceUrls serviceUrls;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RefundItemService refundItemService;

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

    private void callPaymentApiToUpdateCreatedRefundsToOnlinePaid(List<RefundResultDto> refundResultDtos) {
        ArrayList<RefundPaymentCommonDto> updatedRefundPayments = new ArrayList<>();
        // Fetch Paytrail secret per refund
        for (RefundResultDto refund : refundResultDtos) {
            try {
                String merchantId = this.refundItemService.findByRefundId(refund.getRefundId()).stream()
                        .map(RefundItem::getMerchantId)
                        .filter(id -> id != null && !id.isEmpty())
                        .findFirst()
                        .orElse(null);

                if (merchantId == null) {
                    log.warn("No valid merchantId found for refundId {}", refund.getRefundId());
                    continue;
                }
                String namespace = refund.getNamespace();

                String url = serviceUrls.getPaymentServiceUrl() + "/refund/paytrail/update-from-paytrail-refund";

                UpdateFromPaytrailRefundDto dto = new UpdateFromPaytrailRefundDto();
                dto.setRefundId(refund.getRefundId());
                dto.setMerchantId(merchantId);
                dto.setNamespace(namespace);

                JSONObject jsonObject = restServiceClient.makePostCall(url, mapper.writeValueAsString(dto));
                RefundPaymentCommonDto response = mapper.readValue(jsonObject.toString(), RefundPaymentCommonDto.class);

                // Optionally log or process the RefundPayment result
                log.info("Successfully updated refund: {}", response.getRefundId());
                updatedRefundPayments.add(response);

            } catch (Exception e) {
                log.error("Failed to fetch paytrailMerchantId for refund orderId {}: {}", refund.getOrderId(), e.getMessage());
            }
        }
    }

    private static List<RefundResultDto> getRefundResultDtos(List<RefundResultDto> matchedRefunds, Set<String> accountedOrderIds) {
        // Filter out unaccounted payments and log the result size
        List<RefundResultDto> unaccountedRefunds = matchedRefunds.stream().filter(Objects::nonNull)
                .filter(refund ->
                        (refund.getRefundGateway() != null && refund.getRefundGateway().equals(PaymentGatewayEnum.INVOICE.toString()))
                                || !accountedOrderIds.contains(refund.getOrderId())
                ).collect(Collectors.toList());

        log.info("Unaccounted refunds found: {}", unaccountedRefunds.size());
        log.info("Unaccounted refund ids: {}",
                unaccountedRefunds.stream()
                        .map(RefundResultDto::getRefundId)
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")));
        log.info("Unaccounted refund gateway null: {}",
                unaccountedRefunds.stream()
                        .filter(Objects::nonNull)
                        .filter(refund -> refund.getRefundGateway() == null)
                        .map(RefundResultDto::getRefundId)
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")));

        return unaccountedRefunds;
    }

    public List<RefundResultDto> findCreatedRefundsAndUpdateStatusFromPaytrail() throws IOException {
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
        List<RefundResultDto> matchedRefunds = searchRefundPaymentService.findRefundsByStatusAndOrderIds(orderIds, "refund_created");
        log.info("Matched refunds with status 'refund_created': {}", matchedRefunds.size());

        if (matchedRefunds.isEmpty()) {
            log.info("No matched refunds found for the unaccounted orders. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch accounted order IDs and log the size
        Set<String> accountedOrderIds = searchRefundAccountingService.getAccountedOrderIds(
                matchedRefunds.stream().map(RefundResultDto::getOrderId).collect(Collectors.toList())
        );
        log.info("Refund accounted order IDs retrieved: {}", accountedOrderIds.size());

        // Filter out unaccounted payments and log the result size
        List<RefundResultDto> refundResultDtos = getRefundResultDtos(matchedRefunds, accountedOrderIds);

        callPaymentApiToUpdateCreatedRefundsToOnlinePaid(refundResultDtos);
        return refundResultDtos;
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
        List<RefundResultDto> matchedRefunds = searchRefundPaymentService.findRefundsByStatusAndOrderIds(orderIds, "refund_paid_online");
        log.info("Matched refunds with status 'refund_paid_online': {}", matchedRefunds.size());

        if (matchedRefunds.isEmpty()) {
            log.info("No matched refunds found for the unaccounted refunds. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch accounted order IDs and log the size
        Set<String> accountedOrderIds = searchRefundAccountingService.getAccountedOrderIds(
                matchedRefunds.stream().map(RefundResultDto::getOrderId).collect(Collectors.toList())
        );
        log.info("Accounted refund order IDs retrieved: {}", accountedOrderIds.size());

        // Filter out unaccounted payments and log the result size
        return getRefundResultDtos(matchedRefunds, accountedOrderIds);
    }

    public List<RefundResultDto> findCreatedRefundsAndUpdateStatusFromPaytrail(LocalDateTime createdAfter) throws IOException {
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
        List<RefundResultDto> matchedRefunds = searchRefundPaymentService.findRefundsByStatusAndOrderIds(orderIds, "refund_created");
        log.info("Matched refunds with status 'refund_created': {}", matchedRefunds.size());

        if (matchedRefunds.isEmpty()) {
            log.info("No matched refunds found for the unaccounted refunds. Returning an empty list.");
            return Collections.emptyList();
        }

        // Fetch accounted order IDs and log the size
        Set<String> accountedOrderIds = searchRefundAccountingService.getAccountedOrderIds(
                matchedRefunds.stream().map(RefundResultDto::getOrderId).collect(Collectors.toList())
        );
        log.info("Accounted refund order IDs retrieved: {}", accountedOrderIds.size());

        // Filter out unaccounted payments and log the result size
        List<RefundResultDto> refundResultDtos = getRefundResultDtos(matchedRefunds, accountedOrderIds);

        callPaymentApiToUpdateCreatedRefundsToOnlinePaid(refundResultDtos);
        return refundResultDtos;
    }
}
