package fi.hel.verkkokauppa.payment.api.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {

	private String orderId;
	private String namespace;
	private String user;
	private String createdAt;
	private String status;
	private String type;
	private String customerFirstName;
	private String customerLastName;
	private String customerEmail;
	private String priceNet;
	private String priceVat;
	private String priceTotal;

	private String subscriptionId;
	private LocalDate accounted;
}
