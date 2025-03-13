package fi.hel.verkkokauppa.order.logic.subscription;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NextDateCalculatorTest {
    private static final Logger logger = Logger.getLogger(NextDateCalculatorTest.class.getName());

    NextDateCalculator nextDateCalculator = new NextDateCalculator();

    @Test
    void testEndOfMonthRenewalForJanuaryToFebruary() {
        // Set up initial order and subscription starting from January 1
        Order order = new Order();
        Subscription subscription = new Subscription();
        subscription.setPeriodUnit("monthly");
        subscription.setPeriodFrequency(1L);

        // Start date for the subscription is Jan 1
        LocalDateTime startDate = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);

        // Calculate the initial end date
        setStartDateAndCalculateNextEndDateAfterRenewal(order, subscription, startDate);

        // Check that the end date is February 1 end of day, 2025
        assertEquals(LocalDateTime.of(2025, Month.FEBRUARY, 1, 23, 59, 59, 999_999_999), order.getEndDate());

        // Calculate renewal end date for March, expecting it to be start of March, 2025
        setStartDateAndCalculateNextEndDateAfterRenewal(order, subscription, order.getEndDate());

        assertEquals(LocalDateTime.of(2025, Month.MARCH, 1, 23, 59, 59, 999_999_999), order.getEndDate());
    }

    @Test
    void testRenewalAfterFebruaryToMarch() {
        // Set up initial order and subscription starting from February 28 in a non-leap year
        Order order = new Order();
        Subscription subscription = new Subscription();
        subscription.setPeriodUnit("monthly");
        subscription.setPeriodFrequency(1L);

        // Start date for the subscription is February 1, 2025 (non-leap year)
        LocalDateTime startDate = LocalDateTime.of(2025, Month.FEBRUARY, 1, 0, 0);

        // Calculate the initial end date for February (non-leap year should end on 28)
        setStartDateAndCalculateNextEndDateAfterRenewal(order, subscription, startDate);

        // Check that the end date is March 1 end of day, 2025
        assertEquals(LocalDateTime.of(2025, Month.MARCH, 1, 23, 59, 59, 999_999_999), order.getEndDate());

        // Calculate renewal for March, expecting it to be April 1 end of day
        setStartDateAndCalculateNextEndDateAfterRenewal(order, subscription, order.getEndDate());

        assertEquals(LocalDateTime.of(2025, Month.APRIL, 1, 23, 59, 59, 999_999_999), order.getEndDate());
    }

    @Test
    void testTwoYearMonthlyRenewals() {
        // Set up initial order and subscription
        Order order = new Order();
        Subscription subscription = new Subscription();
        subscription.setPeriodUnit("monthly");
        subscription.setPeriodFrequency(1L);

        // Start date for the subscription is Jan 1, 2025
        LocalDateTime startDate = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);

        // Expected end date initially starts at Jan 1, 2025
        LocalDateTime expectedEndDate = startDate;

        // Iterate through 24 months of renewals
        for (int i = 0; i < 24; i++) {
            // Log the period and start date
            logger.info("Period " + (i + 1) + ": Start date = " + expectedEndDate);

            // Calculate the next renewal end date
            setStartDateAndCalculateNextEndDateAfterRenewal(order, subscription, expectedEndDate);

            // Move to the next expected end date
            expectedEndDate = calculateExpectedEndDate(expectedEndDate);

            // Log the calculated end date and expected end date
            logger.info("Period " + (i + 1) + ": Calculated end date = " + order.getEndDate());
            logger.info("Period " + (i + 1) + ": Expected end date = " + expectedEndDate.with(LocalTime.MAX));
            logger.info("Period " + (i + 1) + ": First renewal date = " + order.getEndDate().minus(3,ChronoUnit.DAYS));

            // Assert that the calculated end date matches the expected end date
            assertEquals(expectedEndDate.with(LocalTime.MAX), order.getEndDate(),
                    "Mismatch on iteration " + (i + 1) + " for end date.");
        }
    }

    // Helper method to calculate the expected end date for the next renewal period
    private LocalDateTime calculateExpectedEndDate(LocalDateTime currentEndDate) {
        // Move to the next month
        LocalDateTime nextEndDate = currentEndDate.plusMonths(1);

        // If current end date is at the last day of the month, adjust to the next month's last day
        if (currentEndDate.getDayOfMonth() == currentEndDate.toLocalDate().lengthOfMonth()) {
            return nextEndDate.withDayOfMonth(nextEndDate.toLocalDate().lengthOfMonth());
        } else {
            // Otherwise, retain the same day of the month
            return nextEndDate.withDayOfMonth(currentEndDate.getDayOfMonth());
        }
    }

    private void setStartDateAndCalculateNextEndDateAfterRenewal(Order order, Subscription subscription, LocalDateTime startDate) {
        startDate = startDate.plus(1, ChronoUnit.DAYS);
        LocalDateTime startOfTheStartDate = startDate.with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay());

        order.setStartDate(startOfTheStartDate);

        // Order end date = start date + subscription cycle eg month
        LocalDateTime endDateAddedPeriodCycle = nextDateCalculator.calculateNextDateTime(
                startOfTheStartDate,
                subscription.getPeriodUnit(),
                subscription.getPeriodFrequency());

        // End datetime: start datetime + 1 month - 1 day(end of the day)
        LocalDateTime newEndDate = nextDateCalculator.calculateNextEndDateTime(
                endDateAddedPeriodCycle,
                subscription.getPeriodUnit()
        );

        order.setEndDate(newEndDate);
    }
}
