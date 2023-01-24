package fi.hel.verkkokauppa.common.events;

public class EventType {
    public static String PAYMENT_PAID = "PAYMENT_PAID";
    public static String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static String SUBSCRIPTION_CREATED = "SUBSCRIPTION_CREATED";
    public static String SUBSCRIPTION_CANCELLED = "SUBSCRIPTION_CANCELLED";
    public static String SUBSCRIPTION_RENEWAL_REQUESTED = "SUBSCRIPTION_RENEWAL_REQUESTED";
    public static String SUBSCRIPTION_RENEWAL_ORDER_CREATED = "SUBSCRIPTION_RENEWAL_ORDER_CREATED";
    public static String SUBSCRIPTION_RENEWAL_VALIDATION_FAILED = "SUBSCRIPTION_RENEWAL_VALIDATION_FAILED";
    public static String SUBSCRIPTION_CARD_EXPIRED = "SUBSCRIPTION_CARD_EXPIRED";
    public static String SUBSCRIPTION_CARD_RENEWAL_CREATED = "SUBSCRIPTION_CARD_RENEWAL_CREATED";
    public static String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static String REFUND_CONFIRMED = "REFUND_CONFIRMED";
    public static String REFUND_PAID = "REFUND_PAID";
    public static String REFUND_FAILED = "REFUND_FAILED";
    public static String INTERNAL_ERROR = "INTERNAL_ERROR";
}
