package fi.hel.verkkokauppa.order.api.cron.search.refund;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import fi.hel.verkkokauppa.common.payment.RefundPaymentCommonDto;
import fi.hel.verkkokauppa.common.payment.dto.UpdateFromPaytrailRefundDto;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.order.api.cron.search.dto.RefundResultDto;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.service.refund.RefundItemService;
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


    private void callPaymentApiToUpdateCreatedRefundsToOnlinePaid(List<RefundResultDto> refundResultDtos) {
        ArrayList<RefundPaymentCommonDto> updatedRefundPayments = new ArrayList<>();
        // Fetch Paytrail secret per refund
        for (RefundResultDto refund : refundResultDtos) {
            try {
                String merchantId = this.refundItemService.resolveMerchantIdFromRefundItems(refund.getRefundId());

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

    private static List<RefundResultDto> getRefundResultDtos(List<RefundResultDto> matchedRefunds, Set<String> accountedRefundIds) {
        List<RefundResultDto> unaccountedRefunds = matchedRefunds.stream().filter(Objects::nonNull)
                .filter(refund ->
                        (refund.getRefundGateway() != null && refund.getRefundGateway().equals(PaymentGatewayEnum.INVOICE.toString()))
                                || !accountedRefundIds.contains(refund.getRefundId())
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

    public List<RefundResultDto> findUnaccountedRefunds() throws IOException {
        String refundStatus = "payment_paid_online";
        return getRefundResultDtos(searchRefundService.findUnaccountedRefunds(), refundStatus);
    }

    private List<RefundResultDto> getRefundResultDtos(List<Refund> unaccountedOrders, String refundStatus) throws IOException {
        List<String> refundIds = getRefundIds(unaccountedOrders);
        if (refundIds.isEmpty()) {
            log.info("No unaccounted refunds found. Returning an empty list.");
            return Collections.emptyList();
        }
        List<RefundResultDto> matchedRefundPayments = searchRefundPaymentService.findRefundsByStatusAndRefundIds(refundIds, refundStatus);
        if (matchedRefundPayments.isEmpty()) {
            log.info("No matched refunds found for status '{}' the unaccounted orders. Returning an empty list.", refundStatus);
            return Collections.emptyList();
        }
        Set<String> accountedRefundIds = searchRefundAccountingService.getAccountedRefundIds(
                matchedRefundPayments.stream().map(RefundResultDto::getOrderId).collect(Collectors.toList())
        );
        return getRefundResultDtos(matchedRefundPayments, accountedRefundIds);
    }


    public List<RefundResultDto> findCreatedRefundsAndUpdateStatusFromPaytrail() throws IOException {
        String refundStatus = "refund_created";
        List<RefundResultDto> refundResultDtos = getRefundResultDtos(searchRefundService.findUnaccountedRefunds(), refundStatus);

        callPaymentApiToUpdateCreatedRefundsToOnlinePaid(refundResultDtos);
        return refundResultDtos;
    }

    public List<RefundResultDto> findCreatedRefundsAndUpdateStatusFromPaytrail(LocalDateTime createdAfter) throws IOException {
        String refundStatus = "refund_created";
        List<RefundResultDto> refundResultDtos = getRefundResultDtos(searchRefundService.findUnaccountedRefunds(createdAfter), refundStatus);

        callPaymentApiToUpdateCreatedRefundsToOnlinePaid(refundResultDtos);
        return refundResultDtos;
    }

    public List<RefundResultDto> findUnaccountedRefunds(LocalDateTime createdAfter) throws IOException {
        String refundStatus = "refund_paid_online";
        return getRefundResultDtos(searchRefundService.findUnaccountedRefunds(createdAfter), refundStatus);
    }

    private static List<String> getRefundIds(List<Refund> unaccountedRefunds) {
        List<String> refundIds = unaccountedRefunds.stream().map(Refund::getRefundId).collect(Collectors.toList());
        log.info("Refund ID:s extracted: {}", refundIds.size());
        return refundIds;
    }

}
