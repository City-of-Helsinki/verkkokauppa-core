package fi.hel.verkkokauppa.order.service.rightOfPurchase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.request.rightOfPurchase.OrderRightOfPurchaseRequest;

import fi.hel.verkkokauppa.common.response.OrderRightOfPurchaseResponse;
import lombok.Data;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
public class OrderRightOfPurchaseService {
    private Logger log = LoggerFactory.getLogger(OrderRightOfPurchaseService.class);

    @Autowired
    private CommonServiceConfigurationClient configurationClient;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String namespace;

    public boolean isActive() {
        return StringUtils.isNotEmpty(
                configurationClient.getRestrictedServiceConfigurationValue(
                        namespace,
                        ServiceConfigurationKeys.ORDER_RIGHT_OF_PURCHASE_IS_ACTIVE
                )
        );
    }

    public boolean hasOrderRightOfPurchaseUrl() {
        return StringUtils.isNotEmpty(getOrderRightOfPurchaseUrl());
    }

    public String getOrderRightOfPurchaseUrl() {
        return configurationClient.getRestrictedServiceConfigurationValue(
                namespace,
                ServiceConfigurationKeys.ORDER_RIGHT_OF_PURCHASE_URL
        );
    }

    public ResponseEntity<Boolean> canPurchase(OrderAggregateDto order) throws JsonProcessingException {
        // The Backend returns TRUE
        // by default if the ORDER_RIGHT_OF_PURCHASE_IS_ACTIVE service configuration is not
        // defined in the service configuration.
        if (!isActive() || !hasOrderRightOfPurchaseUrl()) {
            return ResponseEntity.ok().body(true);
        }

        List<OrderItemDto> items = order.getItems();

        for (OrderItemDto orderItemDto : items) {
            String body;
            try {
                // format payload, message to json string conversion
                body = objectMapper.writeValueAsString(OrderRightOfPurchaseRequest.fromOrderItemDto(
                        orderItemDto,
                        order.getOrder().getUser(),
                        namespace)
                );
            } catch (JsonProcessingException e) {
                // Return false if invalid json processing.
                log.error("order-item-id [" + orderItemDto.getOrderItemId() + "] json-processing-failed {}", e);
                return ResponseEntity.ok().body(false);
            }

            JSONObject response = restServiceClient.postQueryJsonService(
                    restServiceClient.getClient(),
                    getOrderRightOfPurchaseUrl(),
                    body
            );

            OrderRightOfPurchaseResponse canPurchaseResponse = objectMapper.readValue(response.toString(), OrderRightOfPurchaseResponse.class);

            ResponseEntity<Boolean> validateResponse = validateResponse(orderItemDto, canPurchaseResponse);
            if (validateResponse != null && Boolean.FALSE.equals(validateResponse.getBody())) {
                return validateResponse;
            }
        }

        return ResponseEntity.ok().body(true);
    }

    public ResponseEntity<Boolean> validateResponse(OrderItemDto orderItemDto, OrderRightOfPurchaseResponse response) {
        if (!response.getErrorMessage().isEmpty()) {
            // Return false if error exists.
            log.info("order-item-id right of purchase [" + orderItemDto.getOrderItemId() + "] response error {}", response);
            return ResponseEntity.ok().body(false);
        }

        return ResponseEntity.ok().body(response.getRightOfPurchase());
    }
}
