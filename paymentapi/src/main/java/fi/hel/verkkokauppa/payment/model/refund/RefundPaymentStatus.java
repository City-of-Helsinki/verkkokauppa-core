package fi.hel.verkkokauppa.payment.model.refund;

public interface RefundPaymentStatus {
	public static String CREATED = "refund_created";
	public static String PAID_ONLINE = "refund_paid_online";
	public static String CANCELLED = "refund_cancelled";;
}

