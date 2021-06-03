package fi.hel.verkkokauppa.order.logic;

import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.model.recurringorder.Period;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class RecurringOrderValidationLogic {

	public void validateQuantityGiven(RecurringOrderDto dto, Errors errors) {
		if (dto.getQuantity() < 1) {
			errors.reject("error.recurringorder.no-quantity"); // TODO: ok?
		}
	}

	public void validateSupportedPeriod(RecurringOrderDto dto, Errors errors) {
		if (!Period.getAllowedPeriods().contains(dto.getPeriodUnit())) {
			errors.reject("error.recurringorder.invalid-period-unit"); // TODO: ok?
		}
	}

	public void validateSupportedPeriodFrequency(RecurringOrderDto dto, Errors errors) {
		if (dto.getPeriodFrequency() < 1 && !dto.getPeriodUnit().equals(Period.ONCE)) {
			errors.reject("error.recurringorder.invalid-period-frequency"); // TODO: ok?
		}
	}
}
