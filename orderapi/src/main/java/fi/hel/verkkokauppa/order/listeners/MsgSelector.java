package fi.hel.verkkokauppa.order.listeners;

public class MsgSelector {

    public static final String DLQ_SELECTOR = "MsgType = 'PAYMENT_PAID' OR MsgType = 'REFUND_PAID'";
}
