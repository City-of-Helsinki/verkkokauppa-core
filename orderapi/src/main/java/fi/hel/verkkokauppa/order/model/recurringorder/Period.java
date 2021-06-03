package fi.hel.verkkokauppa.order.model.recurringorder;

import java.util.HashSet;
import java.util.Set;

public final class Period {
    public static final String ONCE = "once";
    public static final String DAILY = "daily";
    public static final String WEEKLY = "weekly";
    public static final String MONTHLY = "monthly";
    public static final String YEARLY = "yearly";

    public static Set<String> getAllowedPeriods() {
        return new HashSet<>() {{
            add(Period.ONCE);
            add(Period.DAILY);
            add(Period.MONTHLY);
            add(Period.WEEKLY);
            add(Period.YEARLY);
        }};
    }
}
