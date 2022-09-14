package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.events.EventType;
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

@Component
@Slf4j
public class DeadLetterQueueListener {

    private static final String EMAIL_TEMPLATE_PATH = "email/template_email_dlq_alert.html";

    @Value("${payment.failed.notification.email:donotreply.checkout@hel.fi}")
    private String paymentFailedNotificationEmail;

    @Autowired
    private ServiceUrls serviceUrls;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private QueueConfigurations queueConfigurations;


    /**
     * A DLQ listener that only consumes messages with a property of MsgType = 'PAYMENT_PAID'
     */
    @JmsListener(destination = "${queue.dlq:DLQ}", selector = "MsgType = 'PAYMENT_PAID'")
    public void consumeMessage(TextMessage textMessage) {
        try {
            log.info("Consuming PaymentMessage from DLQ: {}", textMessage.getText());
            PaymentMessage message = getPaymentMessageFromTextMessage(textMessage);

            logMessageData(message);

            paymentFailedToProcessAction(message);
        } catch(JsonProcessingException | JMSException jsonProcessingException) {
            log.debug(jsonProcessingException.getMessage());
            log.info("Failed to convert queue message to PaymentMessage type.");
        }
    }

    /**
     * A DLQ listener that only consumes messages with a property of MsgType = 'PAYMENT_PAID'
     * <p>
     *  NOTE: This listener is only used in local development,
     *  because the default DLQ in ActiveMQ is named "ActiveMQ.DLQ" instead of "DLQ".
     * </p>
     */
    @Profile("local")
    @JmsListener(destination = "${queue.dlq:ActiveMQ.DLQ}", selector = "MsgType = 'PAYMENT_PAID'")
    public void consumeMessageLocal(TextMessage textMessage) {
        try {
            log.info("Consuming PaymentMessage from local DLQ: {}", textMessage.getText());
            PaymentMessage message = getPaymentMessageFromTextMessage(textMessage);

            logMessageData(message);

            paymentFailedToProcessAction(message);
        } catch(JsonProcessingException | JMSException jsonProcessingException) {
            log.debug(jsonProcessingException.getMessage());
            log.info("Failed to convert queue message to PaymentMessage type.");
        }
    }

    private void paymentFailedToProcessAction(PaymentMessage message) {
        log.info("Starting payment-failed-to-process action for payment: {}", message.toString());
        sendNotificationToEmail(message);
        sendNotificationService.sendToQueue(message, queueConfigurations.getPaymentFailedToProcessQueue());
        log.info("Ending payment-failed-to-process action for payment: {}", message.toString());
    }

    private void sendNotificationToEmail(PaymentMessage paymentMessage) {
        JSONObject msgJson = new JSONObject();
        msgJson.put("id", paymentMessage.getPaymentId());
        msgJson.put("receiver", paymentFailedNotificationEmail);
        msgJson.put("header", "DLQ queue alert - " + EventType.PAYMENT_PAID);
        log.info("Initial mail message without body: {}", msgJson.toString());
        try {
            String html = Files.readString(Paths.get(ClassLoader.getSystemResource(EMAIL_TEMPLATE_PATH).toURI()));
            html = html.replace("#EVENT_TYPE#", paymentMessage.getEventType());
            html = html.replace("#GENERAL_INFORMATION#", "<p>" + paymentMessage.getPaymentId() + "</p>" );
            html = html.replace("#NAMESPACE#", paymentMessage.getNamespace());
            html = html.replace("#EVENT_PAYLOAD#", mapper.writeValueAsString(paymentMessage));

            msgJson.put("body", html);
        } catch (IOException | URISyntaxException e) {
            log.info("Failed to serialize email template: {}", e.toString());
        }

        log.info("Payment with id {} failed. Sending email notification to {}", paymentMessage.getPaymentId(), paymentFailedNotificationEmail);
        restServiceClient.makePostCall(serviceUrls.getMessageServiceUrl() + "/message/send/email", msgJson.toString());
    }

    private PaymentMessage getPaymentMessageFromTextMessage(TextMessage textMessage) throws JMSException, JsonProcessingException {
        String jsonMessage = textMessage.getText();
        log.info(DeadLetterQueueListener.class + "{}", jsonMessage);

        return mapper.readValue(jsonMessage, PaymentMessage.class);
    }

    /**
     * Logs the DLQ message
     */
    private void logMessageData(PaymentMessage message) throws JsonProcessingException {
        log.info("DLQ-Message: " + mapper.writeValueAsString(message));
    }

}
