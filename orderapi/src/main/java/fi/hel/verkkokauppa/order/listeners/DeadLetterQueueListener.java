package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentFailedMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import lombok.extern.slf4j.Slf4j;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Component
@Slf4j
public class DeadLetterQueueListener {

    private static final String EMAIL_TEMPLATE_PATH = "email/template_email_dlq_alert.html";

    @Value("${payment.failed.notification.email:donotreply.checkout@hel.fi}")
    private String paymentFailedNotificationEmail;

    @Value("${message.service.url}")
    private String messageServiceUrl;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private QueueConfigurations queueConfigurations;

    @JmsListener(destination = "${queue.dlq:DLQ}")
    public void consumeMessage(TextMessage textMessage) throws Exception {
        try {
            PaymentMessage message = getPaymentMessageFromTextMessage(textMessage);

            logMessageData(message);

            if (message.getEventType().equals("PAYMENT_PAID")) {
                paymentFailedAction(message);
            }
        } catch(JsonProcessingException jsonProcessingException) {
            log.debug(jsonProcessingException.getMessage());
            log.info("Failed to convert queue message to PaymentMessage type.");
        }
    }

    /*
     * This listener is only used in local development,
     * because the default DLQ in ActiveMQ is named "ActiveMQ.DLQ" instead of "DLQ".
     */
    @Profile("local")
    @JmsListener(destination = "${queue.dlq:ActiveMQ.DLQ}")
    public void consumeMessageLocal(TextMessage textMessage) throws Exception {
        try {
            PaymentMessage message = getPaymentMessageFromTextMessage(textMessage);

            logMessageData(message);

            if (message.getEventType().equals(EventType.PAYMENT_PAID)) {
                paymentFailedAction(message);
            }
        } catch(JsonProcessingException jsonProcessingException) {
            log.debug(jsonProcessingException.getMessage());
            log.info("Failed to convert queue message to PaymentMessage type.");
        }
    }

    private void paymentFailedAction(PaymentMessage message) {
        PaymentFailedMessage paymentFailedMessage = PaymentFailedMessage.builder()
                .eventType(EventType.PAYMENT_FAILED)
                .paymentId(message.getPaymentId())
                .eventTimestamp(LocalDateTime.now().toString())
                .namespace(message.getNamespace())
                .build();

        sendNotificationToEmail(paymentFailedMessage);
        sendNotificationService.sendToQueue(paymentFailedMessage, queueConfigurations.getPaymentFailedToProcessQueue());
    }

    private void sendNotificationToEmail(PaymentFailedMessage paymentFailedMessage) {
        JSONObject msgJson = new JSONObject();
        msgJson.put("id", paymentFailedMessage.getPaymentId());
        msgJson.put("receiver", paymentFailedNotificationEmail);
        msgJson.put("header", "DLQ queue alert - " + EventType.PAYMENT_FAILED);
        try {
            String html = Files.readString(Paths.get(ClassLoader.getSystemResource(EMAIL_TEMPLATE_PATH).toURI()));
            html = html.replace("#EVENT_TYPE#", paymentFailedMessage.getEventType());
            html = html.replace("#PAYMENT_ID#", paymentFailedMessage.getPaymentId());
            html = html.replace("#NAMESPACE#", paymentFailedMessage.getNamespace());
            html = html.replace("#EVENT_PAYLOAD#", mapper.writeValueAsString(paymentFailedMessage));

            msgJson.put("body", html);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        log.info("Payment with id {} failed. Sending email notification to {}", paymentFailedMessage.getPaymentId(), paymentFailedNotificationEmail);
        restServiceClient.makePostCall(messageServiceUrl + "send/email", msgJson.toString());
    }

    private PaymentMessage getPaymentMessageFromTextMessage(TextMessage textMessage) throws JMSException, JsonProcessingException {
        String jsonMessage = textMessage.getText();
        log.info(DeadLetterQueueListener.class + "{}", jsonMessage);

        return mapper.readValue(jsonMessage, PaymentMessage.class);
    }

    /**
     * Logs eventType, orderId and paymentId from PaymentMessage
     */
    private void logMessageData(PaymentMessage message) throws JsonProcessingException {
        log.info(mapper.writeValueAsString(message));
        log.info("Message eventType: {}, orderId: {}, paymentId: {}", message.getEventType(), message.getOrderId(), message.getPaymentId());
    }

}
