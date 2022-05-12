package fi.hel.verkkokauppa.order.api.admin;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCardExpiredDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCriteria;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionCancellationCause;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.service.renewal.SubscriptionRenewalService;
import fi.hel.verkkokauppa.order.service.subscription.*;
import fi.hel.verkkokauppa.shared.exception.EntityNotFoundException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SubscriptionAdminController {
    private Logger log = LoggerFactory.getLogger(SubscriptionAdminController.class);

    @Value("${subscription.renewal.check.threshold.days}")
    private int subscriptionRenewalCheckThresholdDays;

    @Value("${subscription.renewal.batch.sleep.millis}")
    private int subscriptionRenewalBatchSleepMillis;

    @Value("${subscription.notification.expiring.card.threshold.days:#{7}}")
    private int subscriptionNotificationExpiringCardThresholdDays;

    @Autowired
    private SearchSubscriptionQuery searchSubscriptionQuery;

    @Autowired
    private CancelSubscriptionCommand cancelSubscriptionCommand;

    @Autowired
    private SubscriptionRenewalService renewalService;

    @Autowired
    private SubscriptionItemMetaService subscriptionItemMetaService;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @Autowired
    private SubscriptionCardExpiredService subscriptionCardExpiredService;

    @GetMapping(value = "/subscription-admin/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubscriptionDto> getSubscription(@RequestParam(value = "id") String id) {
        try {
            final SubscriptionDto subscription = getSubscriptionQuery.findById(id);
            subscription.setMeta(subscriptionService.findMetasBySubscriptionId(id));
            if (subscription.getEndDate() != null) {
                subscription.setRenewalDate(subscription.getEndDate().minusDays(subscriptionRenewalCheckThresholdDays));
            }
            return ResponseEntity.ok(subscription);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (EntityNotFoundException e) {
            log.error("Exception on getting Subscription order", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping(value = "/subscription-admin/get-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SubscriptionDto>> getSubscriptions(@RequestParam(value = "userId") String userId) {
        try {
            final List<SubscriptionDto> subscriptions = subscriptionService.findByUser(userId);
            return ResponseEntity.ok(subscriptions);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-subscriptions", "failed to get subscription with user id [" + userId + "]")
            );
        }
    }

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
        while (renewalService.renewalRequestsExist()) {
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
            log.info("Start renewing subscription with subscriptionId: {}", subscriptionId);
            renewalService.renewSubscription(subscriptionId);
            renewalService.finishRenewingSubscription(subscriptionId);
        }
        saveHistoryService.saveSubscriptionMessageHistory(message);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/subscription-admin/renewal-validation-failed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> subscriptionRenewalValidationFailedCallback(@RequestBody SubscriptionMessage message) {
        log.debug("subscription-api received SUBSCRIPTION_RENEWAL_VALIDATION_FAILED event for subscriptionId: " + message.getSubscriptionId());
        String subscriptionId = message.getSubscriptionId();
        try {
            JSONObject result = subscriptionService.sendSubscriptionPaymentFailedEmail(subscriptionId);
        } catch (Exception e) {
            log.error("Error sending paymentFailedEmail for subscription {}", subscriptionId, e);
        }
        saveHistoryService.saveSubscriptionMessageHistory(message);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/subscription-admin/validation-failed-email-sent-increment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Integer> subscriptionValidationFailedEmailSentIncrement(
            @RequestParam(value = "subscriptionId") String subscriptionId
    ) {
        log.debug("subscription-api /subscription-admin/validation-failed-email-sent for subscriptionId: {}", subscriptionId);
        try {
            Subscription subscription = subscriptionService.incrementValidationFailedEmailSentCount(subscriptionId);
            return ResponseEntity.ok().body(subscription.getValidationFailedEmailSentCount());
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting subscription failed, subscriptionId: " + subscriptionId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-subscription", "failed to get subscription with id [" + subscriptionId + "]")
            );
        }
    }


    /**
     * It creates a new `SubscriptionCardExpiredDto` object and returns it as a response
     *
     * @param subscriptionId the id of the subscription
     * @param namespace the namespace of the subscription
     * @return A subscriptionCardExpiredDto object
     */
    @GetMapping(value = "/subscription-admin/create-card-expired-email-entity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubscriptionCardExpiredDto> subscriptionCreateCardExpiredEmailEntity(
            @RequestParam(value = "subscriptionId") String subscriptionId,
            @RequestParam(value = "namespace") String namespace
            ) {
        log.debug("subscription-api /subscription-admin/create-card-expired-email-entity for subscriptionId: {}", subscriptionId);
        try {
            SubscriptionCardExpiredDto cardExpiredDto = subscriptionCardExpiredService.createAndTransformToDto(
                    subscriptionId,
                    namespace
                    );
            return ResponseEntity.ok().body(cardExpiredDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting subscription failed, subscriptionId: " + subscriptionId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-subscription", "failed to get subscription with id [" + subscriptionId + "]")
            );
        }
    }

    @PostMapping(value = "/subscription-admin/set-item-meta", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderItemMetaDto>> setItemMeta(@RequestParam(value = "subscriptionId") String subscriptionId, @RequestParam(value = "orderItemId") String orderItemId, @RequestBody List<OrderItemMetaDto> meta) {
        subscriptionItemMetaService.removeItemMetas(subscriptionId, orderItemId);

        List<OrderItemMetaDto> setMeta = new ArrayList<>();
        meta.forEach(m -> {
            m.setOrderItemId(orderItemId);
            if (m.getOrderItemMetaId() == null) {
                m.setOrderItemMetaId(UUIDGenerator.generateType4UUID().toString());
            }
            m = subscriptionItemMetaService.addItemMeta(m, subscriptionId);
            setMeta.add(m);
        });
        return ResponseEntity.ok().body(setMeta);
    }

    public List<SubscriptionDto> getRenewableSubscriptions() {
        LocalDate currentDate = LocalDate.now();
        LocalDate validityCheckDate = currentDate.plusDays(subscriptionRenewalCheckThresholdDays);
        log.debug("validityCheckDate: {}", validityCheckDate);

        SubscriptionCriteria criteria = new SubscriptionCriteria();
        criteria.setEndDateBefore(validityCheckDate);

        return searchSubscriptionQuery.searchActive(criteria);
    }

    private void cancelExpiredSubscriptions(List<SubscriptionDto> subscriptions) {
        for (SubscriptionDto subscription : subscriptions) {
            LocalDateTime endDate = subscription.getEndDate();

            if (endDate != null) {
                LocalDateTime now = LocalDateTime.now();
                if (endDate.isBefore(now) && !DateTimeUtil.isSameDay(endDate, now)) {
                    String subscriptionId = subscription.getSubscriptionId();
                    log.debug("Subscription with id {} is expired, setting status to {}", subscriptionId, SubscriptionStatus.CANCELLED);
                    cancelSubscriptionCommand.cancel(subscription.getSubscriptionId(), subscription.getUser(), SubscriptionCancellationCause.EXPIRED);
                }
            }
        }
    }

    /**
     * It checks for subscriptions with expiring cards and triggers an event for each of them
     *
     * @return A list of subscriptions with expiring cards.
     */
    @GetMapping(value = "/subscription-admin/check-expiring-card", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SubscriptionDto>> checkExpiringCards() {
        log.debug("Checking expiring cards...");

        List<SubscriptionDto> expiredCardSubscriptions = getSubscriptionsWithExpiringCard();

        // No need to further handle expired ones
        expiredCardSubscriptions.removeIf(s -> s.getStatus() != null && s.getStatus().equalsIgnoreCase(SubscriptionStatus.CANCELLED));
        log.debug("Expiring card subscriptions size: {}", expiredCardSubscriptions.size());

        // Removing all the expired card subscriptions that have already been sent a reminder today.
        expiredCardSubscriptions.removeIf(dto -> {
            List<SubscriptionCardExpiredDto> dtos = subscriptionCardExpiredService
                    .findAllBySubscriptionIdOrderByCreatedAtDesc(dto.getSubscriptionId());
            // If no dtos found we can send reminder
            if (dtos.isEmpty()) {
                return false;
            }

            SubscriptionCardExpiredDto sentCardExpiredNotification = dtos.get(0);
            return DateTimeUtil.isSameDay(LocalDateTime.now(), sentCardExpiredNotification.getCreatedAt());
        });

        expiredCardSubscriptions.forEach(subscriptionDto -> {
            String subscriptionId = subscriptionDto.getSubscriptionId();
            subscriptionService.triggerSubscriptionExpiredCardEvent(
                    subscriptionService.findById(subscriptionId)
            );
        });

        return ResponseEntity.ok(expiredCardSubscriptions);
    }

    /**
     * "Get all active subscriptions that are expiring in the next X days."
     *
     * The function is called from a scheduled task that runs every X day
     *
     * @return A list of subscriptions with expiring cards.
     */
    public List<SubscriptionDto> getSubscriptionsWithExpiringCard() {
        LocalDate currentDate = LocalDate.now();
        LocalDate validityCheckDate = currentDate.plusDays(subscriptionNotificationExpiringCardThresholdDays);
        log.debug("validityCheckDate: {}", validityCheckDate);

        SubscriptionCriteria criteria = new SubscriptionCriteria();
        criteria.setStatus(SubscriptionStatus.ACTIVE);

        criteria.setEndDateBefore(validityCheckDate);

        List<SubscriptionDto> subscriptionDtos = searchSubscriptionQuery.searchActive(criteria);

        return subscriptionDtos
                .stream()
                .filter(subscriptionDto -> subscriptionService.isExpiringCard(currentDate, subscriptionDto))
                .collect(Collectors.toList());
    }

}
