package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.logic.subscription.SubscriptionMappingLogic;
import fi.hel.verkkokauppa.order.logic.subscription.SubscriptionValidationLogic;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
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
		subscription.setCreatedAt(Instant.now());

		// TODO Check this, should this come from order and not from orderItems!
		if (subscription.getOrderItemStartDate() == null) {
			subscription.setOrderItemStartDate(LocalDateTime.now()); // TODO: ok?
		}

		// TODO: end date?
	}
}
