package fi.hel.verkkokauppa.order.api.data.subscription;

import fi.hel.verkkokauppa.shared.model.impl.BaseIdentifiableDto;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class SubscriptionDto extends BaseIdentifiableDto implements Serializable {

    private static final long serialVersionUID = 7841002984877186380L;

    private String status;
    private String customerId;
    private MerchantDto merchant;
    private AddressDto billingAddress; // TODO POISTA!
    // TODO Lisää asiakkaan tiedot
    private String paymentMethod;
    private String paymentMethodToken;
    private LocalDateTime startDate; // TODO: aika myös?
    private LocalDateTime nextDate;// TODO: aika myös?
    private LocalDateTime endDate;// TODO: aika myös?
    private String periodUnit;
    private Long periodFrequency;
    private ProductDto product; // TODO tämä on orderItems Päätasolle productid ja productname
    private Integer quantity;
    private Integer failureCount;
    private Integer currentBillingCycle;
    private Integer numberOfBillingCycles;
    private Set<String> relatedOrderIds;
}