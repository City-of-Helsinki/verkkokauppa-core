package fi.hel.verkkokauppa.order.api.data.recurringorder;

import fi.hel.verkkokauppa.shared.model.impl.BaseIdentifiableDto;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class RecurringOrderDto extends BaseIdentifiableDto implements Serializable {

    private static final long serialVersionUID = 7841002984877186380L;

    private String status;
    private String customerId;
    private MerchantDto merchant;
    private AddressDto billingAddress;
    private AddressDto shippingAddress;
    private Integer daysPastDue; // TODO: ok?
    private String paymentMethod;
    private String paymentMethodToken;
    private String shippingMethod;
    private LocalDate startDate; // TODO: aika myös?
    private LocalDate nextDate;// TODO: aika myös?
    private LocalDate endDate;// TODO: aika myös?
    private LocalDate pauseStartDate;// TODO: aika myös?
    private LocalDate pauseEndDate;// TODO: aika myös?
    private String periodUnit;
    private Long periodFrequency;
    private ProductDto product;
    private String priceNet;
    private String priceVat;
    private String priceTotal;
    private Integer quantity;
    private Integer failureCount;
    private Integer currentBillingCycle;
    private Integer numberOfBillingCycles;
    private LocalDate paidThroughDate;
    private Set<String> relatedOrderIds;
}