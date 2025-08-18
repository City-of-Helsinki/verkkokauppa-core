package fi.hel.verkkokauppa.order.test.utils.payment;

import lombok.Data;

import java.util.List;


@Data
public class TestPaytrailRefundResponse {
    private String transactionId;
    private String provider;
    private String status;
}
