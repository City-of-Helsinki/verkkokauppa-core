package fi.hel.verkkokauppa.order.api.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCriteria;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionCancellationCause;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.service.renewal.SubscriptionRenewalService;
import fi.hel.verkkokauppa.order.service.subscription.CancelSubscriptionCommand;
import fi.hel.verkkokauppa.order.service.subscription.SearchSubscriptionQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class SubscriptionAdminController {
    private Logger log = LoggerFactory.getLogger(SubscriptionAdminController.class);

    @Value("${subscription.renewal.check.threshold.days}")
    private int subscriptionRenewalCheckThresholdDays;

    @Value("${subscription.renewal.batch.sleep.millis}")
    private int subscriptionRenewalBatchSleepMillis;

    @Autowired
    private SearchSubscriptionQuery searchSubscriptionQuery;

    @Autowired
    private CancelSubscriptionCommand cancelSubscriptionCommand;

    @Autowired
    private SubscriptionRenewalService renewalService;


    @GetMapping(value = "/subscription-admin/check-renewals", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> checkRenewals() {
        log.debug("Checking renewals...");

        List<SubscriptionDto> renewableSubscriptions = getRenewableSubscriptions();
        cancelExpiredSubscriptions(renewableSubscriptions);
        // No need to further handle expired ones
        renewableSubscriptions.removeIf(s -> s.getStatus().equalsIgnoreCase(SubscriptionStatus.CANCELLED));
        log.debug("renewable subscriptions size: {}", renewableSubscriptions.size());

        if (!renewalService.renewalRequestsExist()) {
            log.debug("creating new subscription renewal requests");
            renewalService.createRenewalRequests(renewableSubscriptions);
            return ResponseEntity.ok().build();
        } else {
            log.warn("all subscription renewal requests not processed yet, not creating new requests");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping(value = "/subscription-admin/start-processing-renewals", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> startProcessingRenewals() {
        while(renewalService.renewalRequestsExist()) {
            try {
                renewalService.batchProcessNextRenewalRequests();
                Thread.sleep(subscriptionRenewalBatchSleepMillis);
            } catch (InterruptedException e) {
                log.error("processing subscription renewals interrupted", e);
            }
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/subscription-admin/clear-renewal-requests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> clearRenewalRequests() {
        renewalService.clearAll();
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/subscription-admin/renewal-requested-event", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> subscriptionRenewalRequestedEventCallback(@RequestBody SubscriptionMessage message) {
        log.debug("subscription-api received SUBSCRIPTION_RENEWAL_REQUESTED event for subscriptionId: " + message.getSubscriptionId());
        String subscriptionId = message.getSubscriptionId();

        if (renewalService.startRenewingSubscription(subscriptionId)) {
            renewalService.renewSubscription(subscriptionId);
            renewalService.finishRenewingSubscription(subscriptionId);
        }

        return ResponseEntity.ok().build();
    }

    public List<SubscriptionDto> getRenewableSubscriptions() {
        LocalDate currentDate = LocalDate.now();
        LocalDate validityCheckDate = currentDate.plusDays(subscriptionRenewalCheckThresholdDays);
        log.debug("validityCheckDate: {}", validityCheckDate);

        SubscriptionCriteria criteria = new SubscriptionCriteria();
        criteria.setStatus(SubscriptionStatus.ACTIVE);
        criteria.setEndDateBefore(validityCheckDate);

        return searchSubscriptionQuery.searchActive(criteria);
    }

    private void cancelExpiredSubscriptions(List<SubscriptionDto> subscriptions) {
        for (SubscriptionDto subscription : subscriptions) {
            LocalDateTime endDate = subscription.getEndDate();

            if (endDate != null && endDate.isBefore(LocalDateTime.now())) {
                String subscriptionId = subscription.getSubscriptionId();
                log.debug("Subscription with id {} is expired, setting status to {}", subscriptionId, SubscriptionStatus.CANCELLED);
                cancelSubscriptionCommand.cancel(subscription.getSubscriptionId(), subscription.getUser(), SubscriptionCancellationCause.EXPIRED);
            }
        }
    }

}
