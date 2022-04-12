package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCardExpiredDto;
import fi.hel.verkkokauppa.order.api.data.transformer.SubscriptionCardExpiredTransformer;
import fi.hel.verkkokauppa.order.model.subscription.email.SubscriptionCardExpired;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionCardExpiredRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class SubscriptionCardExpiredService {

    @Autowired
    private SubscriptionCardExpiredRepository subscriptionCardExpiredRepository;

    @Autowired
    private SubscriptionCardExpiredTransformer subscriptionCardExpiredTransformer;

    public SubscriptionCardExpiredDto createAndTransformToDto(String subscriptionId, String namespace) {
        SubscriptionCardExpired cardExpired = SubscriptionCardExpired.builder()
                .subscriptionCardExpiredId(
                        UUIDGenerator.generateType4UUID().toString()
                )
                .subscriptionId(subscriptionId)
                .namespace(namespace)
                .createdAt(LocalDateTime.now())
                .build();

        return subscriptionCardExpiredTransformer.transformToDto(
                subscriptionCardExpiredRepository.save(cardExpired)
        );
    }
}
