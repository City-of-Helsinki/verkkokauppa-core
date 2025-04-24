package fi.hel.verkkokauppa.message.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.message.dto.*;
import fi.hel.verkkokauppa.message.enums.MessageTypes;
import fi.hel.verkkokauppa.message.model.Message;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class MessageService {

    private final Logger log = LoggerFactory.getLogger(MessageService.class);
    @Autowired
    private Environment env;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private RestServiceClient restServiceClient;

    @Value("${order.service.url:http://order-api:8080}")
    private String orderServiceUrl;

    @Value("${payment.service.url:http://payment-api:8080}")
    private String paymentServiceUrl;

    @Value("${merchant.service.url:http://merchant-api:8080}")
    private String merchantServiceUrl;

    public Message createSendableEmailMessage(MessageDto messageDto) {
        return new Message(
                UUIDGenerator.generateType4UUID() + ":" + messageDto.getId(),
                messageDto.getBody(),
                messageDto.getReceiver(),
                env.getRequiredProperty("email.bcc"),
                messageDto.getHeader(),
                messageDto.getId(),
                MessageTypes.EMAIL,
                messageDto.getAttachments()
        );
    }

    public void sendEmail(Message message) throws MailException, MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        boolean multipart = message.getAttachments().size() > 0;
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, "utf-8");

        // split string of email addresses delimited with comma
        String[] receivers = message.getSendTo().split(",");
        helper.setTo(receivers);

        helper.setText(message.getMessageText(), true);
        helper.setBcc(env.getRequiredProperty("email.bcc"));
        helper.setSubject(message.getHeader());
        helper.setFrom(message.getFrom());

        for (Map.Entry<String, String> entry : message.getAttachments().entrySet()) {
            byte[] attachment = Base64.getDecoder().decode(entry.getValue());
            helper.addAttachment(entry.getKey(), new ByteArrayResource(attachment));
        }

        log.info(String.valueOf(mimeMessage));
        javaMailSender.send(mimeMessage);
    }

    public GenerateOrderConfirmationPDFRequestDto getPDFRequestDto(String orderId) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        GenerateOrderConfirmationPDFRequestDto dto = new GenerateOrderConfirmationPDFRequestDto();
        dto.setOrderId(orderId);

        try {

            // get Order
            JSONObject orderResponse = restServiceClient.makeAdminGetCall(orderServiceUrl + "/order-admin/get/?orderId=" + orderId);
            OrderAggregateDto orderDto = objectMapper.readValue(orderResponse.toString(), OrderAggregateDto.class);

            // check that there are items
            List<OrderItemDto> items = orderDto.getItems();
            if (items == null || items.isEmpty()) {
                throw new Exception("Order has not items");
            }

            dto.setItems(items);
            dto.setCustomerFirstName(orderDto.getOrder().getCustomerFirstName());
            dto.setCustomerLastName(orderDto.getOrder().getCustomerLastName());
            dto.setCustomerEmail(orderDto.getOrder().getCustomerEmail());

            // Get Payment
            JSONObject paymentResponse = restServiceClient.makeAdminGetCall(paymentServiceUrl + "/payment-admin/online/get?orderId=" + orderId);
            PaymentDto paymentDto = objectMapper.readValue(paymentResponse.toString(), PaymentDto.class);
            dto.setPayment(paymentDto);

            // Get merchant
            String merchantId = orderDto.getItems().get(0).getMerchantId();
            String nameSpace = orderDto.getOrder().getNamespace();
            JSONObject merchantResponse = restServiceClient.makeAdminGetCall(merchantServiceUrl + "/merchant/get?merchantId=" + merchantId + "&namespace=" + nameSpace);
            MerchantDto merchantDto = objectMapper.readValue(merchantResponse.toString(), MerchantDto.class);
            merchantDto.getConfigurations().forEach(configuration -> {
                if (configuration.getKey().equalsIgnoreCase("merchantEmail")) {
                    dto.setMerchantEmail(configuration.getValue());
                } else if (configuration.getKey().equalsIgnoreCase("merchantCity")) {
                    dto.setMerchantCity(configuration.getValue());
                } else if (configuration.getKey().equalsIgnoreCase("merchantBusinessId")) {
                    dto.setMerchantBusinessId(configuration.getValue());
                } else if (configuration.getKey().equalsIgnoreCase("merchantPhone")) {
                    dto.setMerchantPhoneNumber(configuration.getValue());
                } else if (configuration.getKey().equalsIgnoreCase("merchantStreet")) {
                    dto.setMerchantStreetAddress(configuration.getValue());
                } else if (configuration.getKey().equalsIgnoreCase("merchantZip")) {
                    dto.setMerchantZipCode(configuration.getValue());
                } else if (configuration.getKey().equalsIgnoreCase("merchantName")) {
                    dto.setMerchantName(configuration.getValue());
                }
            });
        } catch (Exception e) {
            log.error("Error occurred while collecting data for PDF Receipt",e);
            throw e;
        }

        return dto;
    }

}
