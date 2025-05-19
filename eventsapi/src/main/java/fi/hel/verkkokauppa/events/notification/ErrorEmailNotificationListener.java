package fi.hel.verkkokauppa.events.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.ErrorMessage;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class ErrorEmailNotificationListener extends BaseEmailNotifier<ErrorMessage> {

    @Value("${error.notification.email:donotreply.checkout@hel.fi}")
    private String receivers;

    private final SendNotificationService sendNotificationService;
    private final QueueConfigurations queueConfigurations;

    @Autowired
    public ErrorEmailNotificationListener(
            RestServiceClient restServiceClient,
            ServiceUrls serviceUrls,
            ObjectMapper mapper,
            SendNotificationService sendNotificationService,
            QueueConfigurations queueConfigurations
    ) {
        super(restServiceClient, serviceUrls, mapper);
        this.sendNotificationService = sendNotificationService;
        this.queueConfigurations = queueConfigurations;
    }

    @JmsListener(destination = "${queue.error.email.notifications:error-email-notifications}")
    void consumeMessage(TextMessage textMessage) {

        log.info("Consuming error-email-notifications");
        try {
            ErrorMessage queueMessage = (ErrorMessage) getObjectFromTextMessage(textMessage, ErrorMessage.class);

            if (EventType.ERROR_EMAIL_NOTIFICATION.equals(queueMessage.getEventType())) {
                log.info("event type is ERROR_EMAIL_NOTIFICATION");
                errorEmailNotificationAction(queueMessage);
            } else {
                // received event of type that will not be handled
                log.debug("Received event of type " + queueMessage.getEventType());
            }
        } catch (JsonProcessingException | JMSException jsonProcessingException) {
            log.info("Failed to convert queue message to ErrorMessage type.");
            log.debug(jsonProcessingException.getMessage());
        } catch (IOException exception) {
            log.info("Failed to send Error Notification Email.");
            log.debug(exception.getMessage());
        }
    }

    // send email to configured parties
    private void errorEmailNotificationAction(ErrorMessage queueMessage) throws IOException {
        sendNotificationToEmail(
                UUID.randomUUID().toString(),
                receivers,
                queueMessage.getHeader(),
                queueMessage.getEventType(),
                queueMessage.getMessage(),
                queueMessage.getCause(),
                queueMessage
        );

        // save error notification to error-email-notifications-sent queue
        sendNotificationService.sendToQueue(queueMessage, queueConfigurations.getErrorEmailNotificationsSentQueue());
    }
}
