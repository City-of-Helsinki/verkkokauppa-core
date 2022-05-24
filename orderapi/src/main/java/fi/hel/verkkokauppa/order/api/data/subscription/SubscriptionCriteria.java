package fi.hel.verkkokauppa.order.api.data.subscription;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class SubscriptionCriteria implements Serializable {

	private static final long serialVersionUID = -8772317895908567093L;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // TODO: aika myös?
	private LocalDate activeAtDate;

	private String customerEmail;
	private String status;
	private String namespace;

	private LocalDate endDateBefore;

	private Short paymentMethodExpirationYear;

	private Byte paymentMethodExpirationMonth;

	// TODO: 2 x address id?
}
