package fi.hel.verkkokauppa.order.logic;

import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.model.recurringorder.Period;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDateTime;

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
		// TODO: some kind of max?
	}

	public void validateStartDate(RecurringOrderDto dto, Errors errors) {
		final LocalDate now = LocalDate.now();

		if (dto.getStartDate() != null && dto.getStartDate().isBefore(ChronoLocalDateTime.from(now))) {
			errors.reject("error.recurringorder.invalid-start-date"); // TODO: ok?
		}
	}
}
