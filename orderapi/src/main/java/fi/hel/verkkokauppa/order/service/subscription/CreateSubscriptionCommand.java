package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.order.logic.subscription.SubscriptionMappingLogic;
import fi.hel.verkkokauppa.order.logic.subscription.SubscriptionValidationLogic;
import fi.hel.verkkokauppa.order.model.subscription.Status;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.service.DefaultCreateEntityCommand;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
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
		//SubscriptionValidationLogic.validateSupportedPeriod(dto, errors); // TODO: commented out only for testing!
		//SubscriptionValidationLogic.validateSupportedPeriodFrequency(dto, errors); // TODO: commented out only for testing!
		//SubscriptionValidationLogic.validateStartDate(dto, errors); // TODO: commented out only for testing!

		assertRequiredParameterNotEmpty(dto.getCustomerId(), "customerId");
		//assertRequiredParameterNotEmpty(dto.getPaymentMethod(), "paymentMethod"); // TODO: commented out only for testing!
		//assertRequiredParameterNotEmpty(dto.getPaymentMethodToken(), "paymentMethodToken"); // TODO: commented out only for testing!
		//assertRequiredParameterNotEmpty(dto.getPriceVat(), "priceVat");
		//assertRequiredParameterNotEmpty(dto.getPriceNet(), "priceNet");
		//assertRequiredParameterNotEmpty(dto.getPriceTotal(), "priceTotal");

		validateMerchant(dto);
		validateProduct(dto);
		//validateAddresses(dto);
	}

	private void validateMerchant(SubscriptionDto dto) {
		assertRequiredParameterPresent(dto.getMerchant(), "merchant");
		assertRequiredParameterNotEmpty(dto.getMerchant().getNamespace(), "merchant.namespace");
	}

	private void validateProduct(SubscriptionDto dto) {
		assertRequiredParameterPresent(dto.getProduct(), "product");
		assertRequiredParameterNotEmpty(dto.getProduct().getId(), "product.id");
	}

	/*private void validateAddresses(SubscriptionDto dto) {
		assertRequiredParameterPresent(dto.getBillingAddress(), "billingAddress");
		assertRequiredParameterPresent(dto.getShippingAddress(), "shippingAddress");
		// TODO: loput validoinnit?
	}*/

	@Override
	protected Subscription mapToEntity(SubscriptionDto dto) {
		Subscription subscription = new Subscription();
		getObjectMapper().mapObject(dto, subscription);

		subscriptionMappingLogic.mapMerchantDataToEntity(dto, subscription);
		subscriptionMappingLogic.mapProductDataToEntity(dto, subscription);

		return subscription;
	}

	@Override
	protected void beforeSave(SubscriptionDto dto, Subscription subscription) {
		super.beforeSave(dto, subscription);

		subscription.setStatus(Status.ACTIVE);
		subscription.setCreatedAt(Instant.now());

		if (subscription.getStartDate() == null) {
			subscription.setStartDate(LocalDateTime.now()); // TODO: ok?
		}

		// TODO: end date?
	}
}
