package fi.helshop.microservice.recurringorder.rest.model;

import fi.helshop.microservice.shared.model.impl.BaseIdentifiableDto;
import fi.helshop.microservice.recurringorder.model.recurringorder.Address;
import fi.helshop.microservice.recurringorder.model.recurringorder.Merchant;
import fi.helshop.microservice.recurringorder.model.recurringorder.Product;
import fi.helshop.microservice.recurringorder.model.recurringorder.RecurringOrder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class RecurringOrderDto extends BaseIdentifiableDto implements Serializable {

    private static final long serialVersionUID = 7841002984877186380L;

    private RecurringOrder.Status status;
    private String customerId;
    private Merchant merchant; // TODO: dto?
    private Address billingAddress; // TODO: dto?
    private Address shippingAddress; // TODO: dto?
    private Integer daysPastDue; // TODO: ok?
    private String paymentMethod;
    private String paymentMethodToken;
    private String shippingMethod;
    private LocalDate startDate; // TODO: aika myös?
    private LocalDate nextDate;// TODO: aika myös? nextBillingDate vois olla parempi nimi
    private LocalDate endDate;// TODO: aika myös?
    private LocalDate pauseStartDate;// TODO: aika myös?
    private LocalDate pauseEndDate;// TODO: aika myös?
    private RecurringOrder.Period periodUnit;
    private Long periodFrequency;
    private Product product; // TODO: dto?
    private BigDecimal price;
    private BigDecimal nextBillAmount;
    private Integer quantity;
    private Integer failureCount;
    private Integer currentBillingCycle;
    private Integer numberOfBillingCycles;
    private LocalDate paidThroughDate;
    private Set<String> relatedOrderIds;
}