package fi.helshop.microservice.recurringorder.logic;

import org.springframework.stereotype.Component;
import fi.helshop.microservice.recurringorder.model.recurringorder.RecurringOrder;

import java.time.LocalDate;

@Component
public class IsDateWithinPausePeriodChecker {

	public boolean isDateWithinPausePeriod(RecurringOrder recurringOrder, LocalDate date) {
		LocalDate pauseStartDate = recurringOrder.getPauseStartDate();
		LocalDate pauseEndDate = recurringOrder.getPauseEndDate();

		if (pauseStartDate == null || pauseEndDate == null) {
			return false;
		}

		return (date.isBefore(pauseStartDate) || date.isEqual(pauseStartDate)) &&
				(date.isAfter(pauseEndDate) || date.isEqual(pauseEndDate));
	}
}
