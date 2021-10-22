package fi.hel.verkkokauppa.events.listener;

import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class PaymentMessageListener {

    private Logger log = LoggerFactory.getLogger(PaymentMessageListener.class);

    @Autowired
    private Environment env;

    @KafkaListener(
            topics = "payments",
            groupId="payments",
            containerFactory="paymentsKafkaListenerContainerFactory")
    void paymentPaidlistener(PaymentMessage message) {
        log.info("paymentPaidlistener [{}]", message);

        if (EventType.PAYMENT_PAID.equals(message.getType())) {
            log.info("event type is PAYMENT_PAID");
            // TODO action
        }

        String paymentServiceUrl = env.getRequiredProperty("payment.service.url");
        log.info("payment.service.url is: " + paymentServiceUrl);

    }

        //TODO read target url to call from env

        //TODO format payload/params and send to target url

}