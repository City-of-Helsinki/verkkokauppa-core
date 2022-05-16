package fi.hel.verkkokauppa.order.model.subscription;

import fi.hel.verkkokauppa.common.contracts.OrderItemSubscriptionFields;
import fi.hel.verkkokauppa.order.interfaces.Customer;
import fi.hel.verkkokauppa.order.interfaces.IdentifiableUser;
import fi.hel.verkkokauppa.order.model.OrderStatus;
import fi.hel.verkkokauppa.shared.model.Identifiable;
import fi.hel.verkkokauppa.shared.model.impl.BaseVersionedEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Document(indexName = "subscriptions")
public class Subscription implements Identifiable, Customer, IdentifiableUser, OrderItemSubscriptionFields {
	@Id
	String subscriptionId;

	@Field(type = FieldType.Text)
	private String orderId;

	@Field(type = FieldType.Date, format = DateFormat.date_time)
	private LocalDateTime createdAt;

	@Field(type = FieldType.Date, format = DateFormat.date_time)
	private LocalDateTime updatedAt;

	@Field(type = FieldType.Date, format = DateFormat.date_time)
	private LocalDateTime cancelledAt;

	@Field(type = FieldType.Text) // TODO: keyword?
	private String status;

	@Field(type = FieldType.Keyword)
	private String namespace;

	@Field(type = FieldType.Text)
	private String merchantName;

	@Field(type = FieldType.Text)
	String customerFirstName;

	@Field(type = FieldType.Text)
	String customerLastName;

	@Field(type = FieldType.Keyword)
	String customerEmail;

	@Field(type = FieldType.Text)
	String customerPhone;

//	@Field(type = FieldType.Integer)
//	private Integer daysPastDue; // TODO: needed?

	@Field(type = FieldType.Text)
	private String paymentMethod;

	@Field(type = FieldType.Text)
	private String paymentMethodToken;

	@Field(type = FieldType.Text)
	private Short paymentMethodExpirationYear;

	@Field(type = FieldType.Text)
	private Byte paymentMethodExpirationMonth;

	@Field(type = FieldType.Text)
	private String paymentMethodCardLastFourDigits;

	@Field(type = FieldType.Keyword)
	String user;

	@Field(type = FieldType.Date, format = DateFormat.date_optional_time)
	private Instant startDate;

	@Field(type = FieldType.Date, format = DateFormat.date_optional_time)
	private LocalDateTime endDate;

	@Field(type = FieldType.Date, format = DateFormat.date_optional_time)
	private LocalDateTime billingStartDate;

	@Field(type = FieldType.Text)
	private String periodUnit;

	@Field(type = FieldType.Long)
	private Long periodFrequency;
	// If not set, subscription does not end.
	@Field(type = FieldType.Long)
	private Integer periodCount;

	@Field(type = FieldType.Text)
	private String productId;

	@Field(type = FieldType.Text)
	private String orderItemId;

	@Field(type = FieldType.Text)
	private String productName;

	@Field(type = FieldType.Text)
	private String productLabel;

	@Field(type = FieldType.Text)
	private String productDescription;

	// Product quantity, not subscription count.
	@Field(type = FieldType.Integer)
	private Integer quantity;

	// Counter for sent validation emails
	@Field(type = FieldType.Integer)
	private Integer validationFailedEmailSentCount;

	@Field(type = FieldType.Text)
	String unit;

	@Field(type = FieldType.Text)
	String vatPercentage;

	@Field(type = FieldType.Text)
	String priceNet;

	@Field(type = FieldType.Text)
	String priceVat;

	@Field(type = FieldType.Text)
	String priceGross;

//	@Field(type = FieldType.Date, format = DateFormat.date)
//	private LocalDate paidThroughDate;

	//	@Field(type = FieldType.Integer)
	//	private Integer failureCount;// TODO needed?

	//	@Field(type = FieldType.Integer)
	//	private Integer currentBillingCycle;// TODO needed?


	public Subscription() {
		this.status = OrderStatus.DRAFT;
	}

	public void updateNextDate() {
		//getNextAvailableDateForSubscription(this, null, true, true);
	}

	public void updateStatus() {
//		if (getNextDate() == null) {
//			setStatus(Status.DONE);
//		}
	}

	@Override
	public void setId(String id) {
		this.subscriptionId = id;
	}

	@Override
	public String getId() {
		return this.subscriptionId;
	}
}
