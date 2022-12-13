package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.ErrorMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.queue.error.exceptions.DLQPaymentMessageProcessingException;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.IOException;

@Component
@Slf4j
public class PaymentMessageDLQListener extends BaseDLQEmailNotifier<PaymentMessage> {

    @Value("${payment.failed.notification.email:donotreply.checkout@hel.fi}")
    private String paymentFailedNotificationEmail;

    private final SendNotificationService sendNotificationService;
    private final SaveHistoryService saveHistoryService;
    private final QueueConfigurations queueConfigurations;

    @Autowired
    public PaymentMessageDLQListener(
            RestServiceClient restServiceClient,
            ServiceUrls serviceUrls,
            ObjectMapper mapper,
            SendNotificationService sendNotificationService,
            SaveHistoryService saveHistoryService,
            QueueConfigurations queueConfigurations
    ) {
        super(restServiceClient, serviceUrls, mapper);
        this.sendNotificationService = sendNotificationService;
        this.saveHistoryService = saveHistoryService;
        this.queueConfigurations = queueConfigurations;
    }

    /**
     * A DLQ listener that only consumes messages with a property of MsgType = 'PAYMENT_PAID'
     */
    @JmsListener(destination = "${queue.dlq:DLQ}", selector = MsgSelector.PAYMENT_PAID)
    public void consumeMessage(TextMessage textMessage) {
        try {
            if (textMessage != null && StringUtils.isNotEmpty(textMessage.getText())) {
                log.info("Consuming PaymentMessage from DLQ: {}", textMessage.getText());
                PaymentMessage message = getPaymentMessageFromTextMessage(textMessage);

                logMessageData(message);

                paymentFailedToProcessAction(message);
            } else {
                log.info("TextMessage.getText() is NULL");
            }
        } catch(JsonProcessingException | JMSException jsonProcessingException) {
            log.debug(jsonProcessingException.getMessage());
            log.info("Failed to convert queue message to PaymentMessage type.");
            ErrorMessage errorMsg = ErrorMessage.builder()
                            .eventType(EventType.INTERNAL_ERROR)
                            .eventTimestamp(DateTimeUtil.getDateTime())
                            .message(jsonProcessingException.getMessage())
                            .cause(jsonProcessingException.toString())
                            .build();
            sendNotificationService.sendToQueue(errorMsg, queueConfigurations.getPaymentFailedToProcessQueue());
            saveHistoryService.saveErrorMessageHistory(errorMsg);
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
    @JmsListener(destination = "${queue.dlq:ActiveMQ.DLQ}", selector = MsgSelector.PAYMENT_PAID)
    public void consumePaymentMessageLocal(TextMessage textMessage) {
        try {
            if (textMessage != null && StringUtils.isNotEmpty(textMessage.getText())) {
                log.info("Consuming PaymentMessage from local DLQ: {}", textMessage.getText());

                PaymentMessage message = getPaymentMessageFromTextMessage(textMessage);

                logMessageData(message);

                paymentFailedToProcessAction(message);
            } else {
                log.info("TextMessage.getText() is NULL");
            }
        } catch(JsonProcessingException | JMSException jsonProcessingException) {
            log.debug(jsonProcessingException.getMessage());
            log.info("Failed to convert queue message to PaymentMessage type.");
            ErrorMessage errorMsg = ErrorMessage.builder()
                    .eventType(EventType.INTERNAL_ERROR)
                    .eventTimestamp(DateTimeUtil.getDateTime())
                    .message(jsonProcessingException.getMessage())
                    .cause(jsonProcessingException.toString())
                    .build();
            sendNotificationService.sendToQueue(errorMsg, queueConfigurations.getPaymentFailedToProcessQueue());
            saveHistoryService.saveErrorMessageHistory(errorMsg);
        }
    }

    private void paymentFailedToProcessAction(PaymentMessage message) throws JsonProcessingException {
        log.info("Starting payment-failed-to-process action for payment: {}", message.toString());
        sendPaymentNotificationToEmail(message);
        sendNotificationService.sendToQueue(message, queueConfigurations.getPaymentFailedToProcessQueue());
        log.info("Ending payment-failed-to-process action for payment: {}", message.toString());
    }

    private void sendPaymentNotificationToEmail(PaymentMessage paymentMessage) throws JsonProcessingException {
        try {
            sendNotificationToEmail(
                    paymentMessage.getPaymentId(),
                    paymentFailedNotificationEmail,
                    paymentMessage.getEventType(),
                    paymentMessage.getPaymentId(),
                    paymentMessage.getNamespace(),
                    paymentMessage
            );
            log.info("Payment with id {} failed. Sending email notification to {}", paymentMessage.getPaymentId(), paymentFailedNotificationEmail);
        } catch (IOException e) {
            log.info("Failed to read email template: {}", mapper.writeValueAsString(e));
            throw new DLQPaymentMessageProcessingException("Failed to read email template - not sending email", paymentMessage);
        }
    }

    private PaymentMessage getPaymentMessageFromTextMessage(TextMessage textMessage) throws JMSException, JsonProcessingException {
        String jsonMessage = textMessage.getText();
        log.info(PaymentMessageDLQListener.class + "{}", jsonMessage);

        return mapper.readValue(jsonMessage, PaymentMessage.class);
    }

    /**
     * Logs the DLQ message
     */
    private void logMessageData(PaymentMessage message) throws JsonProcessingException {
        log.info("DLQ-Message: " + mapper.writeValueAsString(message));
    }

}
