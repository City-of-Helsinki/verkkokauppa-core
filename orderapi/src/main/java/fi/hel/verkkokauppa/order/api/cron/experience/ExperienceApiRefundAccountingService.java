package fi.hel.verkkokauppa.order.api.cron.experience;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ExperienceUrls;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.order.api.cron.search.dto.RefundResultDto;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExperienceApiRefundAccountingService {

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ExperienceUrls experienceUrls;

    @Autowired
    private ObjectMapper objectMapper;

    public void sendCreateRefundAccountingRequests(List<RefundResultDto> paymentResults) {
        String url = experienceUrls.getPaymentExperienceUrl() + "admin/refund-accounting-create";

        for (RefundResultDto paymentResult : paymentResults) {
            try {
                // Create JSON body with orderId and refundId
                String requestBody = objectMapper.writeValueAsString(Map.of(
                        "orderId", paymentResult.getOrderId(),
                        "refundId", paymentResult.getRefundId()
                ));
                log.info("Calling admin/refund-accounting-create {}", requestBody);
                // Send POST request
                JSONObject response = restServiceClient.makeAdminPostCall(url, requestBody);

                // Log response and check if response is not null
                if (response != null) {
                    log.info("Response for orderId {} and refundId {}: {}",
                            paymentResult.getOrderId(), paymentResult.getRefundId(), response);
                } else {
                    log.info("Response for orderId {} and refundId {} was null",
                            paymentResult.getOrderId(), paymentResult.getRefundId());
                }

            } catch (Exception e) {
                log.error("Failed to send create accounting request for orderId {} and refundId {}",
                        paymentResult.getOrderId(), paymentResult.getRefundId(), e);
            }
        }
    }

}
