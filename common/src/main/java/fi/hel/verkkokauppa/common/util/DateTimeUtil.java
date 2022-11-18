package fi.hel.verkkokauppa.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class DateTimeUtil {
    private static final String DATE_TIME_WITH_MILLISECONDS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final DateTimeFormatter DEFAULT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd[ HH:mm:ss]")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    public static String getDate() {
        return getFormattedDate(LocalDate.now());
    }

    public static String getFormattedDate(LocalDate localDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        return getFormattedDate(localDate, formatter);
    }

    public static String getFormattedDate(String localDateString, DateTimeFormatter formatter) {
        LocalDate localDate = LocalDate.parse(localDateString);
        return getFormattedDate(localDate, formatter);
    }

    public static String getFormattedDate(LocalDate localDate, DateTimeFormatter formatter) {
        return localDate.format(formatter);
    }

    public static String getDateTime() {
        return getFormattedDateTime(LocalDateTime.now());
    }

    public static String getFormattedDateTime(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String dateWithCommonFormatting = localDateTime.format(formatter);

        return dateWithCommonFormatting;
    }

    public static LocalDateTime getFormattedDateTime() {
        return fromFormattedDateTimeString(getDateTime());
    }

    public static LocalDateTime fromFormattedDateTimeString(String localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return LocalDateTime.parse(localDateTime, formatter);
    }

    public static LocalDateTime fromFormattedDateTimeOptionalString(String localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_WITH_MILLISECONDS);

        String caseStartDate = formatter.format(ZonedDateTime.now());
        System.out.println(caseStartDate);

        return LocalDateTime.parse(localDateTime, formatter);
    }

    public static LocalDateTime fromFormattedDateString(String localDate) {
        return LocalDateTime.parse(localDate, DEFAULT_FORMATTER);
    }

    public static boolean isSameDay(LocalDateTime date1, LocalDateTime date2) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        return fmt.format(date1).equals(fmt.format(date2));
    }

}
