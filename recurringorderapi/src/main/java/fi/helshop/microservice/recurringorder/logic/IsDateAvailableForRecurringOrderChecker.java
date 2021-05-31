package fi.helshop.microservice.recurringorder.logic;

import fi.helshop.microservice.recurringorder.model.recurringorder.RecurringOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class IsDateAvailableForRecurringOrderChecker {

	private final IsDateWithinPausePeriodChecker isDateWithinPausePeriodChecker;

	@Autowired
	public IsDateAvailableForRecurringOrderChecker(IsDateWithinPausePeriodChecker isDateWithinPausePeriodChecker) {
		this.isDateWithinPausePeriodChecker = isDateWithinPausePeriodChecker;
	}

	public boolean isDateAvailableForRecurringOrder(
			RecurringOrder recurringOrder,
			LocalDate date
	) {
		return isDateAvailableForRecurringOrder(recurringOrder, date, null);
	}

	public boolean isDateAvailableForRecurringOrder(
			RecurringOrder recurringOrder,
			LocalDate date,
			LocalDate dateAfter
	) {
		return isDateAvailableForRecurringOrder(recurringOrder, date, null, false);
	}

	public boolean isDateAvailableForRecurringOrder(
			RecurringOrder recurringOrder,
			LocalDate date,
			LocalDate dateAfter,
			boolean ignoreExcludeDate
	) {
		return isDateAvailableForRecurringOrder(recurringOrder, date, null, false, false);
	}

	public boolean isDateAvailableForRecurringOrder(
			RecurringOrder recurringOrder,
			LocalDate date,
			LocalDate dateAfter,
			boolean ignoreExcludeDate,
			boolean ignorePausePeriod
	) {

		return (ignorePausePeriod || !isDateWithinPausePeriodChecker.isDateWithinPausePeriod(recurringOrder, date)) &&
				(ignoreExcludeDate/* || !recurringOrder.isExcludeDate(date)*/) /* TODO! */ &&
				(dateAfter == null || !date.isBefore(dateAfter)); // TODO: ok?
	}
}
