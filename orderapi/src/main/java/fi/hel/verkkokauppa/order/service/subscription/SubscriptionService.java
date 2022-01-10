package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.constants.PaymentType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.EncryptorUtil;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.subscription.PaymentCardInfoDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.subscription.UpdatePaymentCardInfoRequest;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionCancellationCause;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class SubscriptionService {
    private Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    @Value("${payment.card_token.encryption.password}")
    private String cardTokenEncryptionPassword;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SendEventService sendEventService;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

    @Autowired
    private UpdateSubscriptionCommand updateSubscriptionCommand;


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

    public void setPaymentMethodCreditCards(Subscription subscription) {
        subscription.setPaymentMethod(PaymentType.CREDIT_CARDS);
        subscriptionRepository.save(subscription);
    }


    public void afterFirstPaymentPaidEventActions(Set<String> subscriptionsFromOrderId, PaymentMessage message) {
        Objects.requireNonNull(subscriptionsFromOrderId).forEach(subscriptionId -> {
            Order order = orderService.findByIdValidateByUser(message.getOrderId(), message.getUserId());
            Subscription subscription = getSubscriptionQuery.findByIdValidateByUser(subscriptionId, message.getUserId());

            orderService.setOrderStartAndEndDate(order, subscription, message);
            setSubscriptionEndDateFromOrder(order, subscription);

            // All subscriptions have payment type "creditcards" for now
            setPaymentMethodCreditCards(subscription);
            updateCardInfoToSubscription(subscriptionId, message);

            triggerSubscriptionCreatedEvent(subscription);
        });
    }

    public void afterRenewalPaymentPaidEventActions(PaymentMessage message, Order order) {
        Subscription subscription = getSubscriptionQuery.findByIdValidateByUser(order.getSubscriptionId(), message.getUserId());

        if (EventType.PAYMENT_PAID.equals(message.getEventType())) {
            setSubscriptionEndDateFromOrder(order, subscription);
            log.debug("subscription renewal paid, updated subscription end date");
        } else {
            log.debug("subscription renewal not paid, not updating subscription, payment message event type: " + message.getEventType() + " renewal order id: " + message.getOrderId());
        }
    }

    public void updateCardInfoToSubscription(String subscriptionId, PaymentMessage message) {
        if (StringUtils.isNotEmpty(message.getEncryptedCardToken())) {
            PaymentCardInfoDto paymentCardInfoDto = new PaymentCardInfoDto(
                    message.getEncryptedCardToken(),
                    message.getCardTokenExpYear(),
                    message.getCardTokenExpMonth()
            );

            UpdatePaymentCardInfoRequest request = new UpdatePaymentCardInfoRequest(subscriptionId, paymentCardInfoDto, message.getUserId());
            // Token is already encrypted in message
            setSubscriptionCardInfoInternal(request, false);
        }
    }

    public void triggerSubscriptionCreatedEvent(Subscription subscription) {
        SubscriptionMessage subscriptionMessage = SubscriptionMessage.builder()
                .eventType(EventType.SUBSCRIPTION_CREATED)
                .namespace(subscription.getNamespace())
                .subscriptionId(subscription.getId())
                .timestamp(DateTimeUtil.getFormattedDateTime(subscription.getCreatedAt()))
                .build();
        sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, subscriptionMessage);
        log.debug("triggered event SUBSCRIPTION_CREATED for subscriptionId: " + subscription.getId());
    }

    public void triggerSubscriptionRenewValidationFailedEvent(Subscription subscription) {
        SubscriptionMessage subscriptionMessage = SubscriptionMessage.builder()
                .eventType(EventType.SUBSCRIPTION_RENEW_VALIDATION_FAILED)
                .namespace(subscription.getNamespace())
                .subscriptionId(subscription.getId())
                .cancellationCause(SubscriptionCancellationCause.EXPIRED)
                .timestamp(DateTimeUtil.getFormattedDateTime(subscription.getCreatedAt()))
                .build();
        sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, subscriptionMessage);
        log.debug("triggered event SUBSCRIPTION_CREATED for subscriptionId: " + subscription.getId());
    }


    public ResponseEntity<Void> setSubscriptionCardInfoInternal(UpdatePaymentCardInfoRequest dto, boolean encryptToken) {
        String subscriptionId = dto.getSubscriptionId();
        String userId = dto.getUser();

        try {
            SubscriptionDto subscriptionDto = getSubscriptionQuery.getOneValidateByUser(subscriptionId, userId);
            PaymentCardInfoDto paymentCardInfoDto = dto.getPaymentCardInfoDto();

            if (encryptToken) {
                String encryptedToken = EncryptorUtil.encryptValue(paymentCardInfoDto.getCardToken(), cardTokenEncryptionPassword);
                subscriptionDto.setPaymentMethodToken(encryptedToken);
            } else {
                subscriptionDto.setPaymentMethodToken(paymentCardInfoDto.getCardToken());
            }

            subscriptionDto.setPaymentMethodExpirationYear(paymentCardInfoDto.getExpYear());
            subscriptionDto.setPaymentMethodExpirationMonth(paymentCardInfoDto.getExpMonth());
            updateSubscriptionCommand.update(subscriptionId, subscriptionDto);

            return ResponseEntity.ok().build();
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("setting payment method token for subscription with id [" + subscriptionId + "] failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-set-payment-method-token-for-subscription",
                            "setting payment method token for subscription with id [" + subscriptionId + "] failed")
            );
        }
    }

    public Subscription findByIdValidateByUser(String subscriptionId, String userId) {
        if (StringUtils.isEmpty(subscriptionId) || StringUtils.isEmpty(userId)) {
            log.error("unauthorized attempt to load subscription, subscriptionId or userId missing");
            Error error = new Error("subscription-not-found-from-backend", "subscription with id [" + subscriptionId + "] and user id ["+ userId +"] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        Subscription subscription = findById(subscriptionId);

        if (subscription == null) {
            Error error = new Error("subscription-not-found-from-backend", "subscription with id [" + subscriptionId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        String subscriptionUserId = subscription.getUser();
        if (subscriptionUserId == null || userId == null || !subscriptionUserId.equals(userId)) {
            log.error("unauthorized attempt to load subscription, userId does not match");
            Error error = new Error("subscription-not-found-from-backend", "subscription with subscription id [" + subscriptionId + "] and user id ["+ userId +"] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        return subscription;
    }

    public Subscription findById(String subscriptionId) {
        Optional<Subscription> mapping = subscriptionRepository.findById(subscriptionId);

        if (mapping.isPresent())
            return mapping.get();

        log.warn("subscription not found, orderId: " + subscriptionId);
        return null;
    }

    public UpdatePaymentCardInfoRequest getUpdatePaymentCardInfoRequest(String subscriptionId, PaymentMessage message) {
        return new UpdatePaymentCardInfoRequest(
                subscriptionId,
                PaymentCardInfoDto.fromPaymentMessage(message),
                message.getUserId()
        );
    }

    public boolean isCardExpired(Subscription subscription) {
        Short expirationYear = subscription.getPaymentMethodExpirationYear();
        Byte expirationMonth = subscription.getPaymentMethodExpirationMonth();

        if (expirationYear != null && expirationMonth != null) {
            LocalDate cardExpirationDate = LocalDate.of(
                    Integer.valueOf(expirationYear),
                    Integer.valueOf(expirationMonth),
                    1
            );
            LocalDate now = LocalDate.now();

            LocalDate today = LocalDate.of(
                    now.getYear(),
                    now.getMonth(),
                    1
            );

            return cardExpirationDate.isBefore(today);
        }
        return false;
    }
}
