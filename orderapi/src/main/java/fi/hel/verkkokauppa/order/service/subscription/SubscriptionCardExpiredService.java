package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCardExpiredDto;
import fi.hel.verkkokauppa.order.api.data.transformer.SubscriptionCardExpiredTransformer;
import fi.hel.verkkokauppa.order.model.subscription.email.SubscriptionCardExpired;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionCardExpiredRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SubscriptionCardExpiredService {

    @Autowired
    private SubscriptionCardExpiredRepository subscriptionCardExpiredRepository;

    @Autowired
    private SubscriptionCardExpiredTransformer subscriptionCardExpiredTransformer;

    public SubscriptionCardExpiredDto createAndTransformToDto(String subscriptionId) {
        SubscriptionCardExpired cardExpired = SubscriptionCardExpired.builder()
                .subscriptionCardExpiredId(subscriptionId)
                .build();

        return subscriptionCardExpiredTransformer.transformToDto(
                subscriptionCardExpiredRepository.save(cardExpired)
        );
    }
}
