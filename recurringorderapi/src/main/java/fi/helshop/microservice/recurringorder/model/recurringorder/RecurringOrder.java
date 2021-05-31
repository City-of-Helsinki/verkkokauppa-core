package fi.helshop.microservice.recurringorder.model.recurringorder;

import fi.helshop.microservice.shared.model.impl.BaseVersionedEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Document(indexName = "recurring_orders")
public class RecurringOrder extends BaseVersionedEntity implements Serializable {

	private static final long serialVersionUID = -8963491435675971922L;

	public enum Status {
		NOT_ACTIVE,
		ACTIVE,
		PAUSED,
		CANCELED,
		DONE
	}

	public enum Period {
		ONCE,
		DAILY,
		WEEKLY,
		MONTHLY,
		YEARLY
	}

	@Field(type = FieldType.Short) // TODO: ok?
	private Status status;

	@Field(type = FieldType.Keyword)
	private String customerId;

	// TODO: field type ok?
	@Field(type = FieldType.Object)
	private Merchant merchant;

	// TODO: field type?
	private Address billingAddress;
	private Address shippingAddress;

	@Field(type = FieldType.Integer)
	private Integer daysPastDue; // TODO: ok?

	@Field(type = FieldType.Text)
	private String paymentMethod;

	@Field(type = FieldType.Text)
	private String paymentMethodToken;

	@Field(type = FieldType.Text)
	private String shippingMethod;

	@Field(type = FieldType.Date)
	private LocalDate startDate; // TODO: aika myös?

	@Field(type = FieldType.Date)
	private LocalDate nextDate;// TODO: aika myös? nextBillingDate vois olla parempi nimi

	@Field(type = FieldType.Date)
	private LocalDate endDate;// TODO: aika myös?

	@Field(type = FieldType.Date)
	private LocalDate pauseStartDate;// TODO: aika myös?

	@Field(type = FieldType.Date)
	private LocalDate pauseEndDate;// TODO: aika myös?

	@Field(type = FieldType.Short) // TODO: ok?
	private Period periodUnit;

	@Field(type = FieldType.Long)
	private Long periodFrequency;

	// TODO: field type?
	private Product product;

	@Field(type = FieldType.Double)
	private BigDecimal price;

	@Field(type = FieldType.Double)
	private BigDecimal nextBillAmount;

	@Field(type = FieldType.Integer)
	private Integer quantity;

	@Field(type = FieldType.Integer)
	private Integer failureCount;

	@Field(type = FieldType.Integer)
	private Integer currentBillingCycle;

	@Field(type = FieldType.Integer)
	private Integer numberOfBillingCycles;

	@Field(type = FieldType.Date)
	private LocalDate paidThroughDate;

	@Field(type = FieldType.Auto) // TODO: ok?
	private Set<String> relatedOrderIds;

	//private List<SubscriptionStatusEvent> statusHistory; // TODO?
	//private Calendar firstBillingDate; // TODO?
	// TODO: discounts? discount arraylist...

	public void updateNextDate() {
		//getNextAvailableDateForRecurringOrder(this, null, true, true);
	}

	public void updateStatus() {
		if (getNextDate() == null) {
			setStatus(Status.DONE);
		}
	}

	// TODO: can cancel? => workflow for allowed status transitions
	// TODO: can pause? => workflow for allowed status transitions

	public void addRelatedOrderId(String id) {
		relatedOrderIds.add(id);
	}
}
