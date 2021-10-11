package fi.hel.verkkokauppa.order.logic;

import org.springframework.stereotype.Component;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;

import java.time.LocalDate;

@Component
public class IsDateWithinPausePeriodChecker {

	public boolean isDateWithinPausePeriod(Subscription subscription, LocalDate date) {
		LocalDate pauseStartDate = subscription.getPauseStartDate();
		LocalDate pauseEndDate = subscription.getPauseEndDate();

		if (pauseStartDate == null || pauseEndDate == null) {
			return false;
		}

		return (date.isBefore(pauseStartDate) || date.isEqual(pauseStartDate)) &&
				(date.isAfter(pauseEndDate) || date.isEqual(pauseEndDate));
	}
}
