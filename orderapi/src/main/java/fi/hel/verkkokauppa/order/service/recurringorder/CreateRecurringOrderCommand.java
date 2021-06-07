package fi.hel.verkkokauppa.order.service.recurringorder;

import fi.hel.verkkokauppa.order.logic.RecurringOrderMappingLogic;
import fi.hel.verkkokauppa.order.logic.RecurringOrderValidationLogic;
import fi.hel.verkkokauppa.order.model.recurringorder.Status;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.service.DefaultCreateEntityCommand;
import fi.hel.verkkokauppa.order.model.recurringorder.RecurringOrder;
import fi.hel.verkkokauppa.order.repository.jpa.RecurringOrderRepository;
import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class CreateRecurringOrderCommand extends DefaultCreateEntityCommand<RecurringOrder, RecurringOrderDto, String> {

	private final RecurringOrderMappingLogic recurringOrderMappingLogic;
	private final RecurringOrderValidationLogic recurringOrderValidationLogic;

	@Autowired
	public CreateRecurringOrderCommand(
			RecurringOrderRepository repository,
			ObjectMapper objectMapper,
			@Qualifier("beanValidator") Validator validator,
			RecurringOrderMappingLogic recurringOrderMappingLogic,
			RecurringOrderValidationLogic recurringOrderValidationLogic
	) {
		super(repository, objectMapper, validator, RecurringOrderDto.class);

		this.recurringOrderMappingLogic = recurringOrderMappingLogic;
		this.recurringOrderValidationLogic = recurringOrderValidationLogic;
	}

	@Override
	protected void validateBeforeSave(RecurringOrderDto dto, RecurringOrder entity, Errors errors) {
		super.validateBeforeSave(dto, entity, errors);

		recurringOrderValidationLogic.validateQuantityGiven(dto, errors);
		//recurringOrderValidationLogic.validateSupportedPeriod(dto, errors); // TODO: commented out only for testing!
		//recurringOrderValidationLogic.validateSupportedPeriodFrequency(dto, errors); // TODO: commented out only for testing!

		assertRequiredParameterNotEmpty(dto.getCustomerId(), "customerId");
		//assertRequiredParameterNotEmpty(dto.getPaymentMethod(), "paymentMethod"); // TODO: commented out only for testing!
		//assertRequiredParameterNotEmpty(dto.getPaymentMethodToken(), "paymentMethodToken"); // TODO: commented out only for testing!
		assertRequiredParameterNotEmpty(dto.getPriceVat(), "priceVat");
		assertRequiredParameterNotEmpty(dto.getPriceNet(), "priceNet");
		assertRequiredParameterNotEmpty(dto.getPriceTotal(), "priceTotal");

		validateMerchant(dto);
		validateProduct(dto);
		//validateAddresses(dto);
	}

	private void validateMerchant(RecurringOrderDto dto) {
		assertRequiredParameterPresent(dto.getMerchant(), "merchant");
		assertRequiredParameterNotEmpty(dto.getMerchant().getNamespace(), "merchant.namespace");
	}

	private void validateProduct(RecurringOrderDto dto) {
		assertRequiredParameterPresent(dto.getProduct(), "product");
		assertRequiredParameterNotEmpty(dto.getProduct().getId(), "product.id");
	}

	/*private void validateAddresses(RecurringOrderDto dto) {
		assertRequiredParameterPresent(dto.getBillingAddress(), "billingAddress");
		assertRequiredParameterPresent(dto.getShippingAddress(), "shippingAddress");
		// TODO: loput validoinnit?
	}*/

	@Override
	protected RecurringOrder mapToEntity(RecurringOrderDto dto) {
		RecurringOrder recurringOrder = new RecurringOrder();
		getObjectMapper().mapObject(dto, recurringOrder);

		recurringOrderMappingLogic.mapMerchantDataToEntity(dto, recurringOrder);
		recurringOrderMappingLogic.mapProductDataToEntity(dto, recurringOrder);
		recurringOrderMappingLogic.mapShippingAddressDataToEntity(dto, recurringOrder);
		recurringOrderMappingLogic.mapBillingAddressDataToEntity(dto, recurringOrder);

		return recurringOrder;
	}

	@Override
	protected void beforeSave(RecurringOrderDto dto, RecurringOrder recurringOrder) {
		super.beforeSave(dto, recurringOrder);

		recurringOrder.setStatus(Status.ACTIVE);
		recurringOrder.setCreatedAt(Instant.now());

		if (recurringOrder.getShippingAddressId() == null || recurringOrder.getShippingAddressId().isEmpty()) {
			recurringOrder.setShippingAddressId(recurringOrder.getBillingAddressId());
			recurringOrder.setShippingAddressData(recurringOrder.getBillingAddressData());
		}
		if (recurringOrder.getStartDate() == null) {
			recurringOrder.setStartDate(LocalDate.now()); // TODO: ok?
		}
		if (recurringOrder.getNextDate() == null) {
			recurringOrder.setNextDate(LocalDate.now()); // TODO: ok?
		}
		// TODO: end date?
	}
}
