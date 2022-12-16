package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.ErrorMessage;
import fi.hel.verkkokauppa.common.events.message.RefundMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.queue.error.exceptions.DLQRefundMessageProcessingException;
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
public class RefundMessageDLQListener extends BaseDLQEmailNotifier<RefundMessage> {

    @Value("${payment.failed.notification.email:donotreply.checkout@hel.fi}")
    private String refundFailedNotificationEmail;

    private final SendNotificationService sendNotificationService;
    private final SaveHistoryService saveHistoryService;
    private final QueueConfigurations queueConfigurations;

    @Autowired
    public RefundMessageDLQListener(
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
     * A DLQ listener that only consumes messages with a property of MsgType = 'REFUND_PAID'
     */
    @JmsListener(destination = "${queue.dlq:DLQ}", selector = MsgSelector.REFUND_PAID)
    public void consumeMessage(TextMessage textMessage) {
        try {
            if (textMessage != null && StringUtils.isNotEmpty(textMessage.getText())) {
                log.info("Consuming RefundMessage from DLQ: {}", textMessage.getText());
                RefundMessage message = getRefundMessageFromTextMessage(textMessage);

                logMessageData(message);

                refundFailedToProcessAction(message);
            } else {
                log.info("TextMessage.getText() is NULL");
            }
        } catch(JsonProcessingException | JMSException jsonProcessingException) {
            log.debug(jsonProcessingException.getMessage());
            log.info("Failed to convert queue message to RefundMessage type.");
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
     * A DLQ listener that only consumes messages with a property of MsgType = 'REFUND_PAID'
     * <p>
     *  NOTE: This listener is only used in local development,
     *  because the default DLQ in ActiveMQ is named "ActiveMQ.DLQ" instead of "DLQ".
     * </p>
     */
    @Profile("local")
    @JmsListener(destination = "${queue.dlq:ActiveMQ.DLQ}", selector = MsgSelector.REFUND_PAID)
    public void consumePaymentMessageLocal(TextMessage textMessage) {
        try {
            if (textMessage != null && StringUtils.isNotEmpty(textMessage.getText())) {
                log.info("Consuming RefundMessage from local DLQ: {}", textMessage.getText());

                RefundMessage message = getRefundMessageFromTextMessage(textMessage);

                logMessageData(message);

                refundFailedToProcessAction(message);
            } else {
                log.info("TextMessage.getText() is NULL");
            }
        } catch(JsonProcessingException | JMSException jsonProcessingException) {
            log.debug(jsonProcessingException.getMessage());
            log.info("Failed to convert queue message to RefundMessage type.");
            ErrorMessage errorMsg = ErrorMessage.builder()
                    .eventType(EventType.INTERNAL_ERROR)
                    .eventTimestamp(DateTimeUtil.getDateTime())
                    .message(jsonProcessingException.getMessage())
                    .cause(jsonProcessingException.toString())
                    .build();
            sendNotificationService.sendToQueue(errorMsg, queueConfigurations.getRefundFailedToProcessQueue());
            saveHistoryService.saveErrorMessageHistory(errorMsg);
        }
    }

    private void refundFailedToProcessAction(RefundMessage message) throws JsonProcessingException {
        log.info("Starting refund-failed-to-process action for refund: {}", message.toString());
        sendRefundNotificationToEmail(message);
        sendNotificationService.sendToQueue(message, queueConfigurations.getRefundFailedToProcessQueue());
        log.info("Ending refund-failed-to-process action for refund: {}", message.toString());
    }

    private void sendRefundNotificationToEmail(RefundMessage refundMessage) throws JsonProcessingException {
        try {
            sendNotificationToEmail(
                    refundMessage.getRefundId(),
                    refundFailedNotificationEmail,
                    refundMessage.getEventType(),
                    refundMessage.getRefundId(),
                    refundMessage.getNamespace(),
                    refundMessage
            );
            log.info("Refund with id {} failed. Sending email notification to {}", refundMessage.getRefundId(), refundFailedNotificationEmail);
        } catch (IOException e) {
            log.info("Failed to read email template: {}", mapper.writeValueAsString(e));
            throw new DLQRefundMessageProcessingException("Failed to read email template - not sending email", refundMessage);
        }
    }

    private RefundMessage getRefundMessageFromTextMessage(TextMessage textMessage) throws JMSException, JsonProcessingException {
        String jsonMessage = textMessage.getText();
        log.info(RefundMessageDLQListener.class + "{}", jsonMessage);

        return mapper.readValue(jsonMessage, RefundMessage.class);
    }

    /**
     * Logs the DLQ message
     */
    private void logMessageData(RefundMessage message) throws JsonProcessingException {
        log.info("DLQ-Message: " + mapper.writeValueAsString(message));
    }

}
