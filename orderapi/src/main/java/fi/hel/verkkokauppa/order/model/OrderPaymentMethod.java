package fi.hel.verkkokauppa.order.model;

import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "order_payment_method")
@Data
public class OrderPaymentMethod {

	@Id
	private String paymentMethodId;

	@Field(type = FieldType.Text)
	private String orderId;

	@Field(type = FieldType.Text)
	private String userId;

	@Field(type = FieldType.Text)
	private String name;

	@Field(type = FieldType.Text)
	private String code;

	@Field(type = FieldType.Text)
	private String group;

	@Field(type = FieldType.Text)
	private String img;

	@Field(type = FieldType.Text)
	private PaymentGatewayEnum gateway;
}
