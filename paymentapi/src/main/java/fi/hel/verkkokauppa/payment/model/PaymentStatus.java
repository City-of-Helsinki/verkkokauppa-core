package fi.hel.verkkokauppa.payment.model;

public interface PaymentStatus {
	public static String CREATED = "payment_created";
	public static String PAID_ONLINE = "payment_paid_online";
	public static String CANCELLED = "payment_cancelled";
	public static String AUTHORIZED = "authorized";
	public static String INVOICE = "payment_invoice";
	public static String FREE = "payment_free";
}

