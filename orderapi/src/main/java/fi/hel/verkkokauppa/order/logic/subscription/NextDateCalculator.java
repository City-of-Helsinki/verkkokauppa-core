package fi.hel.verkkokauppa.order.logic.subscription;

import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class NextDateCalculator {

	private final static int DATE_SEARCH_LIMIT = 50000;

	private final IsDateAvailableForSubscriptionChecker isDateAvailableForSubscriptionChecker;

	@Autowired
	public NextDateCalculator(
			IsDateAvailableForSubscriptionChecker isDateAvailableForSubscriptionChecker
	) {
		this.isDateAvailableForSubscriptionChecker = isDateAvailableForSubscriptionChecker;
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

	public LocalDate getNextAvailableDateForSubscription(
			Subscription subscription,
			LocalDate dateAfter,
			boolean ignoreExcludeDate
	) {
		String periodUnit = subscription.getPeriodUnit();
		long periodFrequency = subscription.getPeriodFrequency();

		int counter = 0;

		while(true) {
			LocalDate date = null;
//			try {
//				date = calculateNextDate(subscription.getNextDate(), periodUnit, periodFrequency);
//			} catch (Exception e) {
//				return null;
//			}

//			if (isDateAvailableForSubscriptionChecker.isDateAvailableForSubscription(subscription, date, dateAfter,ignoreExcludeDate)) {
//				return date;
//			}
			if (counter++ > DATE_SEARCH_LIMIT) {
				return null;
			}
		}
	}
}
