package fi.hel.verkkokauppa.events.api;

import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootTest
@Slf4j
public class SendEventControllerTest {

    @Autowired
    SendEventController sendEventController;

//    @Test
    void sendPaymentMessage() {
        PaymentMessage orderPaymentMessage = PaymentMessage.builder()
                .eventType(EventType.PAYMENT_PAID)
                .eventTimestamp(LocalDateTime.now().toString())
                .namespace("venepaikat")
                .paymentId("1234")
                .orderId("4e6ccc41-0323-380a-948d-49d07f4cce16")
                .userId("dummy_user")
                .paymentPaidTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .orderType("order")
                .build();
        sendEventController.sendPaymentMessage(orderPaymentMessage);
        orderPaymentMessage.setOrderType("subscription");
        sendEventController.sendPaymentMessage(orderPaymentMessage);
        Assertions.assertTrue(true);
    }

//    @Test
    void sendSubscriptionMessage() {
        SubscriptionMessage subscriptionMessage = SubscriptionMessage.builder()
                .eventType(EventType.SUBSCRIPTION_CANCELLED)
                .cancellationCause("TEST_CANCELLATION_CAUSE")
                .timestamp(DateTimeUtil.getDateTime())
                .subscriptionId("0e5a9ce4-68b6-3f35-9713-1cb80e68c4fd")
                .build();
        sendEventController.sendSubscriptionMessage(subscriptionMessage);
        Assertions.assertTrue(true);
    }

//    @Test
    void sendOrderMessage() {
        OrderMessage orderMessage = OrderMessage.builder()
                .eventType(EventType.ORDER_CANCELLED)
                .namespace("venepaikat")
                .orderId("4e6ccc41-0323-380a-948d-49d07f4cce16")
                .userId("dummy_user")
                .orderType("order")
                .priceTotal("1")
                .priceNet("1")
                .priceVat("0")
                .build();
        sendEventController.sendOrderMessage(orderMessage);
        Assertions.assertTrue(true);
    }
}
