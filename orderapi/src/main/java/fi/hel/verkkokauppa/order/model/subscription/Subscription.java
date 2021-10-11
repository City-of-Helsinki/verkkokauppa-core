package fi.hel.verkkokauppa.order.model.subscription;

import fi.hel.verkkokauppa.shared.model.impl.BaseVersionedEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Document(indexName = "subscriptions")
public class Subscription extends BaseVersionedEntity implements Serializable {

	private static final long serialVersionUID = -8963491435675971922L;

	@Field(type = FieldType.Text) // TODO: keyword?
	private String status;

	@Field(type = FieldType.Keyword)
	private String customerId;

	@Field(type = FieldType.Keyword)
	private String merchantNamespace;

	@Field(type = FieldType.Text)
	private String merchantName;

	@Field(type = FieldType.Text)
	private String billingAddressId;

	@Field(type = FieldType.Text)
	private String shippingAddressId;

	@Field(type = FieldType.Text)
	private String billingAddressData;

	@Field(type = FieldType.Text)
	private String shippingAddressData;

	@Field(type = FieldType.Integer)
	private Integer daysPastDue; // TODO: ok?

	@Field(type = FieldType.Text)
	private String paymentMethod;

	@Field(type = FieldType.Text)
	private String paymentMethodToken;

	@Field(type = FieldType.Text)
	private String shippingMethod;

	@Field(type = FieldType.Date, format = DateFormat.date)
	private LocalDate startDate; // TODO: aika myös? timezone?

	@Field(type = FieldType.Date, format = DateFormat.date)
	private LocalDate nextDate;// TODO: aika myös? timezone?

	@Field(type = FieldType.Date, format = DateFormat.date)
	private LocalDate endDate; // TODO: aika myös? timezone?

	@Field(type = FieldType.Date, format = DateFormat.date)
	private LocalDate pauseStartDate; // TODO: aika myös? timezone?

	@Field(type = FieldType.Date, format = DateFormat.date)
	private LocalDate pauseEndDate; // TODO: aika myös? timezone?

	@Field(type = FieldType.Text)
	private String periodUnit;

	@Field(type = FieldType.Long)
	private Long periodFrequency;

	@Field(type = FieldType.Text)
	private String productId;

	@Field(type = FieldType.Text)
	private String productName;

	@Field(type = FieldType.Text)
	private String priceNet;

	@Field(type = FieldType.Text)
	private String priceVat;

	@Field(type = FieldType.Text)
	private String priceTotal;

	@Field(type = FieldType.Integer)
	private Integer quantity;

	@Field(type = FieldType.Integer)
	private Integer failureCount;

	@Field(type = FieldType.Integer)
	private Integer currentBillingCycle;

	@Field(type = FieldType.Integer)
	private Integer numberOfBillingCycles;

	@Field(type = FieldType.Date, format = DateFormat.date)
	private LocalDate paidThroughDate;

	@Field(type = FieldType.Auto) // TODO: ok?
	private Set<String> relatedOrderIds;

	//private BigDecimal nextBillAmount; // TODO?
	//private List<SubscriptionStatusEvent> statusHistory; // TODO?
	//private Calendar firstBillingDate; // TODO?
	// TODO: discounts? discount arraylist...

	public void updateNextDate() {
		//getNextAvailableDateForSubscription(this, null, true, true);
	}

	public void updateStatus() {
		if (getNextDate() == null) {
			setStatus(Status.DONE);
		}
	}

	// TODO: can cancel? => workflow/SubscriptionStatusLogic for allowed status transitions
	// TODO: can pause? => workflow/SubscriptionStatusLogic for allowed status transitions

	public void addRelatedOrderId(String id) {
		relatedOrderIds.add(id);
	}
}
