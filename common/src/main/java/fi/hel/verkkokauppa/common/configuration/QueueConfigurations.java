package fi.hel.verkkokauppa.common.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Data
public class QueueConfigurations {

    @Value("${queue.order.notifications:order-notifications}")
    String orderNotificationsQueue;

    @Value("${queue.subscription.notifications:subscription-notifications}")
    String subscriptionNotificationsQueue;

    @Value("${queue.payment.notifications:payment-notifications}")
    String paymentNotificationsQueue;

    @Value("${queue.refund.notifications:refund-notifications}")
    String refundNotificationsQueue;

    @Value("${queue.payment.failed.to.process:payment-failed-to-process}")
    String paymentFailedToProcessQueue;

    @Value("${queue.refund.failed.to.process:refund-failed-to-process}")
    String refundFailedToProcessQueue;

    @Value("${queue.subscription.renewals:subscription-renewals}")
    String subscriptionRenewalsQueue;

    @Value("${queue.error.email.notifications:error-email-notifications}")
    String errorEmailNotificationsQueue;

    @Value("${queue.error.email.notifications.sent:error-email-notifications-sent}")
    String errorEmailNotificationsSentQueue;

    public List<String> getAll() {
        ArrayList<String> allQueues = new ArrayList<>();
        allQueues.add(orderNotificationsQueue);
        allQueues.add(subscriptionNotificationsQueue);
        allQueues.add(paymentNotificationsQueue);
        allQueues.add(subscriptionRenewalsQueue);
        allQueues.add(paymentFailedToProcessQueue);
        allQueues.add(refundNotificationsQueue);
        allQueues.add(refundFailedToProcessQueue);
        allQueues.add(errorEmailNotificationsQueue);
        allQueues.add(errorEmailNotificationsSentQueue);
        return allQueues;
    }
}
