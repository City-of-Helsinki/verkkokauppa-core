package fi.hel.verkkokauppa.order.logic.subscription;

import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDateTime;

@Component
public class SubscriptionValidationLogic {

	public void validateQuantityGiven(SubscriptionDto dto, Errors errors) {
		if (dto.getQuantity() < 1) {
			errors.reject("error.subscription.no-quantity"); // TODO: ok?
		}
	}

	public void validateSupportedPeriod(SubscriptionDto dto, Errors errors) {
		if (!Period.getAllowedPeriods().contains(dto.getPeriodUnit())) {
			errors.reject("error.subscription.invalid-period-unit"); // TODO: ok?
		}
	}

	public void validateSupportedPeriodFrequency(SubscriptionDto dto, Errors errors) {
		if (dto.getPeriodFrequency() < 1 && !dto.getPeriodUnit().equals(Period.ONCE)) {
			errors.reject("error.subscription.invalid-period-frequency"); // TODO: ok?
		}
		// TODO: some kind of max?
	}

	public void validateStartDate(SubscriptionDto dto, Errors errors) {
		final LocalDate now = LocalDate.now();

		if (dto.getOrderItemStartDate() != null && dto.getOrderItemStartDate().isBefore(ChronoLocalDateTime.from(now))) {
			errors.reject("error.subscription.invalid-start-date"); // TODO: ok?
		}
	}
}
