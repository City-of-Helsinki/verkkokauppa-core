package fi.hel.verkkokauppa.order.logic.subscription;

import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

@Component
public class NextDateCalculator {

	public LocalDateTime calculateNextDateTime(LocalDateTime date, String periodUnit, long periodFrequency) {
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

	public LocalDateTime calculateNextEndDateTime(LocalDateTime date, String periodUnit) {
		switch (periodUnit) {
			case Period.DAILY:
			case Period.YEARLY:
			case Period.WEEKLY:
				// Return back the original date unmodified
				return date;
			case Period.MONTHLY:
				// End datetime: start datetime + 1 month - 1day(end of the day)
				LocalDateTime minusOneDay = date.minus(1, ChronoUnit.DAYS);

				LocalDateTime endOfDay = minusOneDay.with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay());
				return endOfDay;
			default:
				throw new IllegalArgumentException("Not supported");
		}
	}
}
