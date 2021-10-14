package fi.hel.verkkokauppa.order.api.data.subscription;

import fi.hel.verkkokauppa.order.interfaces.Customer;
import fi.hel.verkkokauppa.order.interfaces.IdentifiableUser;
import fi.hel.verkkokauppa.order.interfaces.Product;
import fi.hel.verkkokauppa.shared.model.impl.BaseIdentifiableDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class SubscriptionDto extends BaseIdentifiableDto implements Serializable, Customer, IdentifiableUser, Product {

    private static final long serialVersionUID = 7841002984877186380L;

    private String status;
    private String paymentMethod;
    private String paymentMethodToken;

    private LocalDateTime orderItemStartDate;
    private LocalDateTime paymentStartDate;
    private LocalDateTime endDate;
//    private LocalDateTime nextDate; // TODO needed?
//    private Integer failureCount; // TODO needed?
//    private Integer currentBillingCycle; // TODO needed?
    private Integer numberOfBillingCycles; // TODO needed?
    private Set<String> relatedOrderIds; // TODO should this be plain orderId?

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
    private String periodUnit;
    private Long periodFrequency;
    private Integer quantity;
}