package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public String generateSubscriptionId(String namespace, String user, String orderItemId, String timestamp) {
        String whoseSubscription = UUIDGenerator.generateType3UUIDString(namespace, user);
        String whoseSubscriptionLink = UUIDGenerator.generateType3UUIDString(whoseSubscription, orderItemId);
        return UUIDGenerator.generateType3UUIDString(whoseSubscriptionLink, timestamp);
    }

    /**
     * This should be called after order end date is updated.
     * Subscription End Date = Order End Date
     */
    public void setSubscriptionEndDateFromOrder(Order order, Subscription subscription) {
        subscription.setEndDate(order.getEndDate());
        // Set Updated at to current time and date.
        subscription.setUpdatedAt(DateTimeUtil.getFormattedDateTime());
        subscriptionRepository.save(subscription);
    }
}
