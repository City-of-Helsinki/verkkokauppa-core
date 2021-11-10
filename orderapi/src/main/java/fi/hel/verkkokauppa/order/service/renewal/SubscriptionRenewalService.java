package fi.hel.verkkokauppa.order.service.renewal;

import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.renewal.SubscriptionRenewalProcess;
import fi.hel.verkkokauppa.order.model.renewal.SubscriptionRenewalRequest;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRenewalProcessRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRenewalRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class SubscriptionRenewalService {
    private Logger log = LoggerFactory.getLogger(SubscriptionRenewalService.class);

    @Value("${subscription.renewal.batch.size}")
    private int subscriptionRenewalBatchSize;

    @Value("${subscription.renewal.batch.sleep.millis}")
    private int subscriptionRenewalBatchSleepMillis;

    @Autowired
    private SubscriptionRenewalRequestRepository requestRepository;

    @Autowired
    private SubscriptionRenewalProcessRepository processRepository;

    @Autowired
    private SendEventService sendEventService;


    public void createRenewalRequests(List<SubscriptionDto> renewableSubscriptions) {
        if (renewableSubscriptions != null && !renewableSubscriptions.isEmpty()) {
            renewableSubscriptions.forEach(subscriptionDto -> {
                String subscriptionId = subscriptionDto.getSubscriptionId();
                SubscriptionRenewalRequest request = SubscriptionRenewalRequest.builder()
                        .id(subscriptionId)
                        .renewalRequested(LocalDateTime.now())
                        .build();
                requestRepository.save(request);
            });
        }
    }

    public boolean renewalRequestsExist() {
        long existingRequestsCount = requestRepository.count();
        return existingRequestsCount > 0 ? true : false;
    }

    public long renewalProcessCount() {
        long currentProcessesCount = processRepository.count();
        return currentProcessesCount;
    }

    public boolean startRenewingSubscription(String subscriptionId) {
        Optional<SubscriptionRenewalRequest> existingRequest = requestRepository.findById(subscriptionId);
        Optional<SubscriptionRenewalProcess> existingProcess = processRepository.findById(subscriptionId);
        if (existingProcess.isPresent() || existingRequest.isEmpty()) {
            // stop, this subscription is already being renewed by another instance of this service
            return false;
        } else {
            requestRepository.deleteById(subscriptionId);
            SubscriptionRenewalProcess process = SubscriptionRenewalProcess.builder().id(subscriptionId).processingStarted(LocalDateTime.now()).build();
            processRepository.save(process);
            return true;
        }
    }

    public void finishRenewingSubscription(String subscriptionId) {
        Optional<SubscriptionRenewalProcess> existingProcess = processRepository.findById(subscriptionId);
        if (existingProcess.isPresent()) {
            processRepository.delete(existingProcess.get());
        }
    }

    public void clearAll() {
        requestRepository.deleteAll();
        processRepository.deleteAll();
    }

    public void batchProcessNextRenewalRequests() throws InterruptedException {
        Page<SubscriptionRenewalRequest> requests = requestRepository.findAll(PageRequest.of(1, subscriptionRenewalBatchSize, Sort.by("renewalRequested").ascending()));
        if (requests != null) {
            requests.getContent().forEach(request -> {
                triggerSubscriptionRenewalEvent(request.getId());
            });

            Thread.sleep(subscriptionRenewalBatchSleepMillis);
            batchProcessNextRenewalRequests();
        }
    }

    private void triggerSubscriptionRenewalEvent(String subscriptionId) {
        SubscriptionMessage subscriptionMessage = SubscriptionMessage.builder()
                .eventType(EventType.SUBSCRIPTION_RENEWAL_REQUESTED)
                .timestamp(DateTimeUtil.getDateTime())
                .subscriptionId(subscriptionId)
                .build();
        sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, subscriptionMessage);
        log.debug("triggered event SUBSCRIPTION_RENEWAL_REQUESTED for subscriptionId: " + subscriptionId);
    }

}