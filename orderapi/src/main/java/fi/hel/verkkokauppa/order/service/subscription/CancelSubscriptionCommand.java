package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.shared.exception.EntityNotFoundException;
import fi.hel.verkkokauppa.shared.service.BaseServiceOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CancelSubscriptionCommand extends BaseServiceOperation {

	private Logger log = LoggerFactory.getLogger(CancelSubscriptionCommand.class);

	@Autowired
	private SendEventService sendEventService;

	private final SubscriptionRepository repository;
	private final GetSubscriptionQuery getSubscriptionQuery;

	@Autowired
	public CancelSubscriptionCommand(
			SubscriptionRepository repository,
			GetSubscriptionQuery getSubscriptionQuery,
			@Qualifier("beanValidator") Validator validator
	) {
		super(validator);
		this.repository = repository;
		this.getSubscriptionQuery = getSubscriptionQuery;
	}

	public Subscription cancel(String id, String userId, String cause) {
		final Subscription subscription = getSubscriptionQuery.findByIdValidateByUser(id, userId);
		LocalDateTime cancelledAt = DateTimeUtil.getFormattedDateTime();
		doCancel(subscription, cancelledAt);
		triggerSubscriptionCancelledEvent(subscription, cause, cancelledAt);

		return subscription;
	}

	private void doCancel(Subscription subscription, LocalDateTime cancelledAt) {
		subscription.setStatus(SubscriptionStatus.CANCELLED);
		subscription.setCancelledAt(cancelledAt);

		repository.save(subscription);
	}

	public void triggerSubscriptionCancelledEvent(Subscription subscription, String cause, LocalDateTime cancelledAt) {
		SubscriptionMessage subscriptionMessage = SubscriptionMessage.builder()
				.eventType(EventType.SUBSCRIPTION_CANCELLED)
				.namespace(subscription.getNamespace())
				.subscriptionId(subscription.getId())
				.timestamp(DateTimeUtil.getFormattedDateTime(cancelledAt))
				.cancellationCause(cause)
				.build();
		sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, subscriptionMessage);
		log.debug("triggered event SUBSCRIPTION_CANCELLED for subscriptionId: " + subscription.getId());
	}

}
