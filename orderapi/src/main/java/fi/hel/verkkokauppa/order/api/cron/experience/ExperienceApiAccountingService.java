package fi.hel.verkkokauppa.order.api.cron.experience;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ExperienceUrls;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.order.api.cron.search.dto.PaymentResultDto;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExperienceApiAccountingService {

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ExperienceUrls experienceUrls;

    @Autowired
    private ObjectMapper objectMapper;

    public void sendCreateAccountingRequests(List<PaymentResultDto> paymentResults) {
        String url = experienceUrls.getPaymentExperienceUrl() + "admin/accounting-create";

        for (PaymentResultDto paymentResult : paymentResults) {
            try {
                // Create JSON body with orderId and paymentId
                String requestBody = objectMapper.writeValueAsString(Map.of(
                        "orderId", paymentResult.getOrderId(),
                        "paymentId", paymentResult.getPaymentId()
                ));

                // Send POST request
                JSONObject response = restServiceClient.makeAdminPostCall(url, requestBody);

                // Log response and check if response is not null
                if (response != null) {
                    log.info("Response for orderId {} and paymentId {}: {}",
                            paymentResult.getOrderId(), paymentResult.getPaymentId(), response);
                } else {
                    log.info("Response for orderId {} and paymentId {} was null",
                            paymentResult.getOrderId(), paymentResult.getPaymentId());
                }

            } catch (Exception e) {
                log.error("Failed to send create accounting request for orderId {} and paymentId {}",
                        paymentResult.getOrderId(), paymentResult.getPaymentId(), e);
            }
        }
    }

}
