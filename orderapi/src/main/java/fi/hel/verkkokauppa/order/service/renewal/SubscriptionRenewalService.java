package fi.hel.verkkokauppa.order.service.renewal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SubscriptionRenewalService {
    private Logger log = LoggerFactory.getLogger(SubscriptionRenewalService.class);

    @Value("${subscription.renewal.event.delay.millis:#{1000}}")
    private int subscriptionRenewalEventDelay;

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

    @Autowired
    private ObjectMapper objectMapper;

    public String renewSubscription(String subscriptionId) {
        String orderId = null;
        final SubscriptionDto subscriptionDto = getSubscriptionQuery.getOne(subscriptionId);

        // multiple calls to this can create new orders even if subscription was just renewed
        // check that end date for subscription is not suspiciously far in the future (KYV-)
        LocalDateTime endDate = subscriptionDto.getEndDate();
        if( endDate.isAfter(LocalDateTime.now().plusDays(5)))
        {
            log.info("End date for subscriptionId: {} is {}. Do not renew yet.", subscriptionId, endDate);
        }
        else
        {
            // end date is close so we can renew
            log.info("End date for subscriptionId: {} is {}. Time to renew.", subscriptionId, endDate);
            orderId = createOrderFromSubscriptionCommand.createFromSubscription(subscriptionDto);
            // If order ids matches prevents sending renewal order created event.
            if (orderId != null && !Objects.equals(subscriptionDto.getOrderId(), orderId)) {
                Order order = orderService.findById(orderId);
                log.info("Trigger order created event for subscriptionId: {}", subscriptionId);
                orderService.triggerOrderCreatedEvent(order, EventType.SUBSCRIPTION_RENEWAL_ORDER_CREATED);
            } else {
                log.info("Order ids doesn't match, can be duplicate, don't create new order for subscriptionId: {}", subscriptionId);
            }
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
        Optional<SubscriptionRenewalProcess> existingProcess = processRepository.findById(subscriptionId);
        if (existingProcess.isPresent()) {
            log.info("There is existing process renewing subscription with subscriptionId: {}", subscriptionId);
            // stop, this subscription is already being renewed by another instance of this service
            return false;
        } else {
            SubscriptionRenewalProcess process = SubscriptionRenewalProcess.builder().id(subscriptionId).processingStarted(LocalDateTime.now()).build();
            processRepository.save(process);
            try {
                requestRepository.deleteById(subscriptionId);
            } catch (Exception e) {
                log.debug("Request deleting failed for {}", subscriptionId);
            }
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

    public AtomicLong batchProcessNextRenewalRequests() {
        log.debug("processing next renewal requests, subscriptionRenewalEventDelay: {}", subscriptionRenewalEventDelay);
        Page<SubscriptionRenewalRequest> requests = requestRepository.findAll(PageRequest.of(0, subscriptionRenewalBatchSize, Sort.by("renewalRequested").ascending()));
        log.debug("renewal requests {}", requests);
        AtomicLong count = new AtomicLong();
        if (requests != null) {
            for (var request : requests.getContent()) {
                triggerSubscriptionRenewalEvent(request.getId());
                requestRepository.deleteById(request.getId());
                count.getAndDecrement();
                // delay before sending next renewal request
                try {
                    Thread.sleep(subscriptionRenewalEventDelay);
                } catch (InterruptedException e) {
                    // swallow possible exception from sleep
                    log.error("Sleep during batch processing renewal requests was interrupted",e);
                    try {
                        Thread.sleep(subscriptionRenewalEventDelay);
                    } catch (InterruptedException ex) {
                        // swallow possible exception from sleep retry
                    }
                }
            }
        }
        return count;
    }

    public ArrayList<SubscriptionRenewalRequest> getAllRequests() {
        ArrayList<SubscriptionRenewalRequest> requests = new ArrayList<>();
        requestRepository.findAll().forEach(requests::add);
        return requests;
    }

    public ArrayList<SubscriptionRenewalProcess> getAllProcesses() {
        ArrayList<SubscriptionRenewalProcess> processes = new ArrayList<>();
        processRepository.findAll().forEach(processes::add);
        return processes;
    }

    public void logAll() {
        try {
            log.error(objectMapper.writeValueAsString(this.getAllProcesses()));
        } catch (JsonProcessingException e) {
            log.error("Could not serialize getAllProcesses");
        }

        try {
            log.error(objectMapper.writeValueAsString(this.getAllRequests()));
        } catch (JsonProcessingException e) {
            log.error("Could not serialize getAllRequests");
        }
    }

    private void triggerSubscriptionRenewalEvent(String subscriptionId) {
        SubscriptionDto subscription = getSubscriptionQuery.findById(subscriptionId);

        SubscriptionMessage subscriptionMessage = SubscriptionMessage.builder()
                .eventType(EventType.SUBSCRIPTION_RENEWAL_REQUESTED)
                .timestamp(DateTimeUtil.getDateTime())
                .subscriptionId(subscriptionId)
                .namespace(subscription.getNamespace())
                .eventTimestamp(DateTimeUtil.getDateTime())
                .build();
        sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, subscriptionMessage);
        log.debug("triggered event SUBSCRIPTION_RENEWAL_REQUESTED for subscriptionId: " + subscriptionId);
    }

}
