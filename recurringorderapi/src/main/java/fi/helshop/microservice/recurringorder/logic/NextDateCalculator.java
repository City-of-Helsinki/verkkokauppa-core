package fi.helshop.microservice.recurringorder.logic;

import fi.helshop.microservice.recurringorder.model.recurringorder.RecurringOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class NextDateCalculator {

	private final static int DATE_SEARCH_LIMIT = 50000;

	private final IsDateAvailableForRecurringOrderChecker isDateAvailableForRecurringOrderChecker;

	@Autowired
	public NextDateCalculator(
			IsDateAvailableForRecurringOrderChecker isDateAvailableForRecurringOrderChecker
	) {
		this.isDateAvailableForRecurringOrderChecker = isDateAvailableForRecurringOrderChecker;
	}

	private LocalDate calculateNextDate(LocalDate date, RecurringOrder.Period periodUnit, long periodFrequency) {
		switch (periodUnit) {
			case DAILY:
				return date.plus(periodFrequency, ChronoUnit.DAYS);
			case WEEKLY:
				return date.plus(periodFrequency, ChronoUnit.WEEKS);
			case MONTHLY:
				return date.plus(periodFrequency, ChronoUnit.MONTHS);
			case YEARLY:
				return date.plus(periodFrequency, ChronoUnit.YEARS);
			default:
				throw new IllegalArgumentException("Not supported");
		}
	}

	public LocalDate getNextAvailableDateForRecurringOrder(
			RecurringOrder recurringOrder,
			LocalDate dateAfter,
			boolean ignoreExcludeDate,
			boolean ignorePausePeriod
	) {
		RecurringOrder.Period periodUnit = recurringOrder.getPeriodUnit();
		long periodFrequency = recurringOrder.getPeriodFrequency();

		int counter = 0;

		while(true) {
			LocalDate date = null;
			try {
				date = calculateNextDate(recurringOrder.getNextDate(), periodUnit, periodFrequency);
			} catch (Exception e) {
				return null;
			}

			if (isDateAvailableForRecurringOrderChecker.isDateAvailableForRecurringOrder(recurringOrder, date, dateAfter, ignoreExcludeDate, ignorePausePeriod)) {
				return date;
			}
			if (counter++ > DATE_SEARCH_LIMIT) {
				return null;
			}
		}
	}
}
