package fi.hel.verkkokauppa.message.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.message.constants.ApiUrls;
import fi.hel.verkkokauppa.message.dto.*;
import fi.hel.verkkokauppa.message.service.OrderConfirmationPDF;
import org.apache.xmpbox.type.BadFieldValueException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import fi.hel.verkkokauppa.message.model.Message;
import fi.hel.verkkokauppa.message.service.MessageService;

import javax.validation.Valid;
import javax.xml.transform.TransformerException;
import java.io.IOException;

@RestController
@Validated
public class MessageController {
    private final Logger log = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService service;

    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private OrderConfirmationPDF orderConfirmationPdf;

    @Autowired
    private RestServiceClient restServiceClient;

    @Value("${order.service.url:http://order-api:8080}")
    private String orderServiceUrl;

    @Value("${payment.service.url:http://payment-api:8080}")
    private String paymentServiceUrl;

    @Value("${test.pdf.save:false}")
    private boolean saveTestPDF;


    @PostMapping(value = ApiUrls.MESSAGE_ROOT + "/send/email", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Message> sendMessage(@RequestBody @Valid MessageDto messageDto
    ) {
        try {
            Message message = service.createSendableEmailMessage(messageDto);
            service.sendEmail(message);
            return new ResponseEntity<>(message, HttpStatus.CREATED);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Sending message failed, id: " + messageDto.getId(), e);
            Error error = new Error("failed-to-send-message", "failed to send message with id [" + messageDto.getId() + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    @PostMapping(value = ApiUrls.MESSAGE_ROOT + "/send/errorNotification", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendErrorNotification(@RequestBody ErrorNotificationDto dto) {
        try {
            sendNotificationService.sendErrorNotification(dto.getMessage(), dto.getCause());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Sending error notification failed {}", dto);
            Error error = new Error("failed-to-send-error-notification", "failed to send error notification");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    @PostMapping(value = ApiUrls.MESSAGE_ROOT + "/pdf/orderConfirmation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> generateOrderConfirmationPdf(@RequestParam(value = "orderId") String orderId) throws IOException, TransformerException, BadFieldValueException {
        ObjectMapper objectMapper = new ObjectMapper();
        GenerateOrderConfirmationPDFRequestDto dto = new GenerateOrderConfirmationPDFRequestDto();
        dto.setOrderId(orderId);

        JSONObject orderResponse = restServiceClient.makeAdminGetCall(orderServiceUrl + "/order-admin/get/?orderId=" + orderId);
        OrderAggregateDto orderDto = objectMapper.readValue(orderResponse.toString(), OrderAggregateDto.class);

        dto.setItems(orderDto.getItems());

        JSONObject paymentResponse = restServiceClient.makeAdminGetCall(paymentServiceUrl + "/payment-admin/online/get?orderId=" + orderId);
        PaymentDto paymentDto = objectMapper.readValue(paymentResponse.toString(), PaymentDto.class);

        dto.setPayment(paymentDto);

        byte[] pdfArray = orderConfirmationPdf.generate("order-confirmation.pdf", dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

//    @PostMapping(value = ApiUrls.MESSAGE_ROOT + "/pdf/orderConfirmation", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<Void> generateOrderConfirmationPdf(@RequestBody GenerateOrderConfirmationPDFRequestDto dto) throws IOException, TransformerException, BadFieldValueException {
//        orderConfirmationPdf.generate("order-confirmation.pdf", dto);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
}
