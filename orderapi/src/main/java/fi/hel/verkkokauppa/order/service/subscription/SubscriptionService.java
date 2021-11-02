package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionService {
    public String generateSubscriptionId(String namespace, String user, String orderItemId, String timestamp) {
        String whoseSubscription = UUIDGenerator.generateType3UUIDString(namespace, user);
        String whoseSubscriptionLink = UUIDGenerator.generateType3UUIDString(whoseSubscription, orderItemId);
        return UUIDGenerator.generateType3UUIDString(whoseSubscriptionLink, timestamp);
    }
}
