package fi.hel.verkkokauppa.events.api;

import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.SendEventService;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SendEventController {

    private Logger log = LoggerFactory.getLogger(SendEventController.class);

    private SendEventService sendEventService;

    @Autowired
    SendEventController(SendEventService sendEventService) {
        this.sendEventService = sendEventService;
    }


    @PostMapping(value = "/event/send/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendPaymentMessage(@RequestBody PaymentMessage message) {
        try {
            sendEventService.sendEventMessage(TopicName.PAYMENTS, message);

            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Sending event failed, paymentId: " + message.getPaymentId(), e);
            Error error = new Error("failed-to-send-event", "failed to send event with paymentId [" + message.getPaymentId() + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    @PostMapping(value = "/event/send/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendSubscriptionMessage(@RequestBody SubscriptionMessage message) {
        try {
            sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, message);

            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Sending event failed, subscriptionId: " + message.getSubscriptionId(), e);
            Error error = new Error("failed-to-send-event", "failed to send event with subscriptionId [" + message.getSubscriptionId() + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    @PostMapping(value = "/event/send/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendOrderMessage(@RequestBody OrderMessage message) {
        try {
            sendEventService.sendEventMessage(TopicName.ORDERS, message);

            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Sending event failed, orderId: " + message.getOrderId(), e);
            Error error = new Error("failed-to-send-event", "failed to send event with orderId [" + message.getOrderId() + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }
}
