package fi.hel.verkkokauppa.order.api.data.subscription;

import fi.hel.verkkokauppa.common.contracts.OrderItemSubscriptionFields;
import fi.hel.verkkokauppa.order.interfaces.Customer;
import fi.hel.verkkokauppa.order.interfaces.IdentifiableUser;
import fi.hel.verkkokauppa.order.interfaces.Product;
import fi.hel.verkkokauppa.shared.model.impl.BaseIdentifiableDto;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class SubscriptionDto extends BaseIdentifiableDto implements Serializable, Customer, IdentifiableUser, Product, OrderItemSubscriptionFields {

    private static final long serialVersionUID = 7841002984877186380L;

    private String status;
    // Date data
    private LocalDateTime startDate; // (created from orderitems.startDate)
    private LocalDateTime billingStartDate; // (created from orderitems.billingStartDate)
    private LocalDateTime endDate;

    // User data
    private String user;
    private String namespace;
    // Customer data
    private String customerPhone;
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    // Product data
    private String productName;
    private String productId;
    private Integer quantity;
    private String orderItemId;
    // Payment data
    private String paymentMethod;
    private String paymentMethodToken;
    private Short paymentMethodExpirationYear;
    private Byte paymentMethodExpirationMonth;
    // Period data (created from orderitems)
    private String periodUnit;
    private Long periodFrequency;
    private Integer periodCount;
    // Price data
    private String priceNet;
    private String priceVat;
    private String priceGross;
    // Relations data
    private Set<String> relatedOrderIds; // "Main" subscription contains all orders created from it.

//    private LocalDateTime nextDate; // TODO needed?
//    private Integer failureCount; // TODO needed?
//    private Integer currentBillingCycle; // TODO needed?

}