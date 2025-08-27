package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.productmapping.dto.ProductMappingDto;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.service.GenerateCsvService;
import fi.hel.verkkokauppa.common.service.dto.CheckPaymentDto;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
@Slf4j
public class PaytrailStatusCheckService {
    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private GenerateCsvService generateCsvService;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ServiceUrls serviceUrls;

    @Autowired
    private ObjectMapper objectMapper;

    public String sendPaytrailStatusCheckReport(List<CheckPaymentDto> updatedPayments, List<String> errors) {
        String message = "";
        String cause = "";
        String header = "Paytrail status check";

        updatedPayments = filterOutByStatus(updatedPayments, "new");

        if (!updatedPayments.isEmpty()) {
            header += " - Updated payments: " + updatedPayments.size();
            message += "Updated " + updatedPayments.size() + " payments ";
            // TODO: create csv data common service
            cause += generateCsvService.generateCsvData(updatedPayments);
            log.info("Payment for order updated in paytrail status check. \n {}", cause);
        }

        if (!errors.isEmpty()) {
            header += " - Errors: " + errors.size();
            cause += "\n\nErrors:\n" + errors;
        }


        if (!message.isEmpty() || !cause.isEmpty()) {
            // send error notification email
            sendNotificationService.sendErrorNotification(
                    message,
                    cause,
                    header
            );
        }

        return cause;
    }

    private static List<CheckPaymentDto> filterOutByStatus(List<CheckPaymentDto> updatedPayments, String status) {
        updatedPayments = updatedPayments
                .stream()
                .filter(Objects::nonNull)
                .filter(checkPaymentDto ->
                        checkPaymentDto.getPaymentProviderStatus() != null &&
                                !checkPaymentDto.getPaymentProviderStatus().equalsIgnoreCase(status)
                )
                .collect(Collectors.toList());
        return updatedPayments;
    }

    public String getMerchantIdByProductId(String productId) throws JsonProcessingException {
        JSONObject productMappingResponse = restServiceClient.makeAdminGetCall(serviceUrls.getProductMappingServiceUrl() + "/get?productId=" + productId);
        ProductMappingDto productMapping = objectMapper.readValue(productMappingResponse.toString(), ProductMappingDto.class);

        return productMapping.getMerchantId();
    }
}
