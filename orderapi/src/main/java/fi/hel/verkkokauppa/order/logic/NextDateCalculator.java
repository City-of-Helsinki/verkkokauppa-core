package fi.hel.verkkokauppa.order.logic;

import fi.hel.verkkokauppa.order.model.recurringorder.Period;
import fi.hel.verkkokauppa.order.model.recurringorder.RecurringOrder;
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

	private LocalDate calculateNextDate(LocalDate date, String periodUnit, long periodFrequency) {
		switch (periodUnit) {
			case Period.DAILY:
				return date.plus(periodFrequency, ChronoUnit.DAYS);
			case Period.WEEKLY:
				return date.plus(periodFrequency, ChronoUnit.WEEKS);
			case Period.MONTHLY:
				return date.plus(periodFrequency, ChronoUnit.MONTHS);
			case Period.YEARLY:
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
		String periodUnit = recurringOrder.getPeriodUnit();
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
