package fi.hel.verkkokauppa.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    public static String getDate() {
        return getFormattedDate(LocalDate.now());
    }

    public static String getFormattedDate(LocalDate localDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        String dateWithCommonFormatting = localDate.format(formatter);

        return dateWithCommonFormatting;
    }

    public static String getDateTime() {
        return getFormattedDateTime(LocalDateTime.now());
    }

    public static String getFormattedDateTime(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String dateWithCommonFormatting = localDateTime.format(formatter);

        return dateWithCommonFormatting;
    }
    
}
