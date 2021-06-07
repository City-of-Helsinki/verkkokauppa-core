package fi.hel.verkkokauppa.order.model.recurringorder;

public interface Status {
    public static String NOT_ACTIVE = "not_active";
    public static String ACTIVE = "active";
    public static String PAUSED = "paused";
    public static String CANCELLED = "cancelled";
    public static String DONE = "done";
}
