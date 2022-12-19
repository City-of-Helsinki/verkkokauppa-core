package fi.hel.verkkokauppa.payment.model.paytrail.payment;

import lombok.Data;
import org.helsinki.paytrail.constants.PaymentMethodGroup;

@Data
public class PaymentMethodGroupDataModel {
    private PaymentMethodGroup id;
    private String name;
    private String icon;
    private String svg;
}
