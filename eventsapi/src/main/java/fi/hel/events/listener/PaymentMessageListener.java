package fi.hel.events.listener;

import fi.hel.verkkokauppa.common.message.PaymentMessage;
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
            topics = "PAYMENT_PAID",
            groupId="payments",
            containerFactory="paymentsKafkaListenerContainerFactory")
    void paymentPaidlistener(PaymentMessage message) {
        log.info("paymentPaidlistener [{}]", message);
    }

        //TODO check event type from message payload

        //TODO read target url to call from env

        //TODO format payload/params and send to target url

}