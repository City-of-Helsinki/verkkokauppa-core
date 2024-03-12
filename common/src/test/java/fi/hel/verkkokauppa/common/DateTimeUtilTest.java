package fi.hel.verkkokauppa.common;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.TimeZone;

public class DateTimeUtilTest {
    @Test
    public void returnsFinnishZonedDateTime() {
        ZonedDateTime ztd = ZonedDateTime.parse("2024-03-12T10:21:40.220968100Z[GMT]");
        assertEquals(DateTimeUtil.toFinnishZonedDateTime(ztd).toString(), "2024-03-12T12:21:40.220968100+02:00[Europe/Helsinki]");
    }
    @Test
    public void returnsFinnishZonedDateTimeFromLocalDateTime() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        LocalDateTime ldt = LocalDateTime.parse("2024-03-12T22:21:40");
        assertEquals(DateTimeUtil.toFinnishZonedDateTime(ldt).toString(), "2024-03-13T00:21:40+02:00[Europe/Helsinki]");
    }

    @Test
    public void returnsFinnishDateFromLocalDateTime() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        LocalDateTime ldt = LocalDateTime.parse("2024-03-12T22:21:40");
        assertEquals(DateTimeUtil.toFinnishDate(ldt).toString(), "2024-03-13");
    }
}
