package fi.hel.verkkokauppa.order.service.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.rest.dto.configuration.MerchantDto;
import fi.hel.verkkokauppa.common.rest.dto.payment.PaymentDto;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.model.pdf.GenerateOrderConfirmationPDFRequestDto;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PdfGenerationService {

    private final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);


    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    CommonServiceConfigurationClient commonServiceConfigurationClient;

    @Value("${payment.service.url:http://payment-api:8080}")
    private String paymentServiceUrl;

    @Value("${merchant.service.url:http://merchant-api:8080}")
    private String merchantServiceUrl;



    public GenerateOrderConfirmationPDFRequestDto getPDFRequestDto(String orderId) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        GenerateOrderConfirmationPDFRequestDto generatePdfDto = new GenerateOrderConfirmationPDFRequestDto();
        generatePdfDto.setOrderId(orderId);

        try {

            // get Order
            OrderAggregateDto orderDto = orderService.orderAggregateDto(orderId).getBody();

            // check that there are items
            List<OrderItemDto> items = orderDto.getItems();
            if (items == null || items.isEmpty()) {
                throw new Exception("Order has not items");
            }

            generatePdfDto.setItems(items);
            generatePdfDto.setCustomerFirstName(orderDto.getOrder().getCustomerFirstName());
            generatePdfDto.setCustomerLastName(orderDto.getOrder().getCustomerLastName());
            generatePdfDto.setCustomerEmail(orderDto.getOrder().getCustomerEmail());

            // Get Payment
            JSONObject paymentResponse = restServiceClient.makeAdminGetCall(paymentServiceUrl + "/payment-admin/online/get?orderId=" + orderId);
            PaymentDto paymentDto = objectMapper.readValue(paymentResponse.toString(), PaymentDto.class);
            generatePdfDto.setPayment(paymentDto);

            // Get merchant
            String merchantId = orderService.getFirstMerchantId(orderDto);
                    // orderDto.getItems().get(0).getMerchantId();
            String nameSpace = orderDto.getOrder().getNamespace();
            updateWithMerchantConfiguration(generatePdfDto, merchantId, nameSpace);

        } catch (Exception e) {
            log.error("Error occurred while collecting data for PDF Receipt",e);
            throw e;
        }

        return generatePdfDto;
    }

    private void updateWithMerchantConfiguration(GenerateOrderConfirmationPDFRequestDto generatePdfDto, String merchantId, String nameSpace){
        MerchantDto merchantDto = commonServiceConfigurationClient.getMerchantModel(merchantId, nameSpace);
        merchantDto.getConfigurations().forEach(configuration -> {
            switch( configuration.getKey().toLowerCase() ){
                case "merchantemail":
                    generatePdfDto.setMerchantEmail(configuration.getValue());
                    break;
                case "merchantcity":
                    generatePdfDto.setMerchantCity(configuration.getValue());
                    break;
                case "merchantbusinessid":
                    generatePdfDto.setMerchantBusinessId(configuration.getValue());
                    break;
                case "merchantphone":
                    generatePdfDto.setMerchantPhoneNumber(configuration.getValue());
                    break;
                case "merchantstreet":
                    generatePdfDto.setMerchantStreetAddress(configuration.getValue());
                    break;
                case "merchantzip":
                    generatePdfDto.setMerchantZipCode(configuration.getValue());
                    break;
                case "merchantname":
                    generatePdfDto.setMerchantName(configuration.getValue());
                    break;
            }
        });
    }

}
