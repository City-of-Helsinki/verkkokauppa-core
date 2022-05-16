package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.logic.subscription.SubscriptionMappingLogic;
import fi.hel.verkkokauppa.order.logic.subscription.SubscriptionValidationLogic;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.service.DefaultCreateEntityCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class CreateSubscriptionCommand extends DefaultCreateEntityCommand<Subscription, SubscriptionDto, String> {

	private final SubscriptionMappingLogic subscriptionMappingLogic;
	private final SubscriptionValidationLogic subscriptionValidationLogic;

	@Autowired
	private SubscriptionService subscriptionService;
	@Autowired
	private SendNotificationService sendNotificationService;

	@Autowired
	public CreateSubscriptionCommand(
			SubscriptionRepository repository,
			ObjectMapper objectMapper,
			@Qualifier("beanValidator") Validator validator,
			SubscriptionMappingLogic subscriptionMappingLogic,
			SubscriptionValidationLogic subscriptionValidationLogic
	) {
		super(repository, objectMapper, validator, SubscriptionDto.class);

		this.subscriptionMappingLogic = subscriptionMappingLogic;
		this.subscriptionValidationLogic = subscriptionValidationLogic;
	}

	@Override
	protected void validateBeforeSave(SubscriptionDto dto, Subscription entity, Errors errors) {
		super.validateBeforeSave(dto, entity, errors);

		subscriptionValidationLogic.validateQuantityGiven(dto, errors);
	}

	@Override
	protected void beforeSave(SubscriptionDto dto, Subscription subscription) {
		super.beforeSave(dto, subscription);
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		LocalDateTime createdAt = LocalDateTime.now();
		subscription.setCreatedAt(createdAt);
		if (subscription.getSubscriptionId() == null) {
			subscription.setSubscriptionId(subscriptionService.generateSubscriptionId(
					dto.getNamespace(),
					dto.getUser(),
					dto.getOrderItemId(),
					createdAt.toString()
			));
		}

		// This value should come from orderItems!
		if (subscription.getStartDate() == null) {
			subscription.setStartDate(Instant.now()); // TODO: ok?
		}

		// TODO: end date?
	}

	@Override
	protected void afterSave(SubscriptionDto dto, Subscription entity) {
		SubscriptionMessage message = SubscriptionMessage.builder()
				.subscriptionId(entity.getSubscriptionId())
				.orderId(entity.getOrderId())
				.namespace(entity.getNamespace())
				.eventType(EventType.SUBSCRIPTION_CREATED)
				.timestamp(entity.getCreatedAt().toString())
				.build();
		sendNotificationService.sendSubscriptionMessageNotification(message);
	}
}
