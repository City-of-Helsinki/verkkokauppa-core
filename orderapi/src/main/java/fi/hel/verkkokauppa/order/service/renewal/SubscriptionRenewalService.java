package fi.hel.verkkokauppa.order.service.renewal;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.renewal.SubscriptionRenewalRequest;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRenewalProcessRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRenewalRequestRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SubscriptionRenewalService {

    @Autowired
    private SubscriptionRenewalRequestRepository requestRepository;

    @Autowired
    private SubscriptionRenewalProcessRepository processRepository;


    public void createRenewalRequests(List<SubscriptionDto> renewableSubscriptions) {
        LocalDateTime renewalRequestedTime = DateTimeUtil.getFormattedDateTime();

        if (renewableSubscriptions != null && !renewableSubscriptions.isEmpty()) {
            renewableSubscriptions.forEach(subscriptionDto -> {
                String subscriptionId = subscriptionDto.getSubscriptionId();
                SubscriptionRenewalRequest request = SubscriptionRenewalRequest.builder()
                        .subscriptionId(subscriptionId)
                        .renewalRequested(renewalRequestedTime)
                        .build();
                requestRepository.save(request);
            });
        }
    }

    public boolean renewalRequestsExist() {
        long existingRequestsCount = requestRepository.count();
        return existingRequestsCount > 0 ? true : false;
    }

}