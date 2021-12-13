package fi.hel.verkkokauppa.order.service.renewal;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.renewal.SubscriptionRenewalProcess;
import fi.hel.verkkokauppa.order.model.renewal.SubscriptionRenewalRequest;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRenewalProcessRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRenewalRequestRepository;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.subscription.CreateOrderFromSubscriptionCommand;
import fi.hel.verkkokauppa.order.service.subscription.GetSubscriptionQuery;
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
import java.util.Objects;
import java.util.Optional;

@Component
public class SubscriptionRenewalService {
    private Logger log = LoggerFactory.getLogger(SubscriptionRenewalService.class);

    @Value("${subscription.renewal.batch.size}")
    private int subscriptionRenewalBatchSize;

    @Autowired
    private SubscriptionRenewalRequestRepository requestRepository;

    @Autowired
    private SubscriptionRenewalProcessRepository processRepository;

    @Autowired
    private SendEventService sendEventService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

    @Autowired
    private CreateOrderFromSubscriptionCommand createOrderFromSubscriptionCommand;

    public String renewSubscription(String subscriptionId) {
        final SubscriptionDto subscriptionDto = getSubscriptionQuery.getOne(subscriptionId);
        String orderId = createOrderFromSubscriptionCommand.createFromSubscription(subscriptionDto);
        // If order ids matches prevents sending renewal order created event.
        if (orderId != null && !Objects.equals(subscriptionDto.getOrderId(), orderId)) {
            Order order = orderService.findById(orderId);
            orderService.triggerOrderCreatedEvent(order, EventType.SUBSCRIPTION_RENEWAL_ORDER_CREATED);
        }

        return orderId;
    }

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

    public void batchProcessNextRenewalRequests() {
        log.debug("processing next renewal requests");
        Page<SubscriptionRenewalRequest> requests = requestRepository.findAll(PageRequest.of(0, subscriptionRenewalBatchSize, Sort.by("renewalRequested").ascending()));
        log.debug("renewal requests {}", requests);
        if (requests != null) {
            requests.getContent().forEach(request -> {
                triggerSubscriptionRenewalEvent(request.getId());
            });
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