package fi.hel.verkkokauppa.payment.model;

import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "payment_method")
@Data
public class PaymentMethod {

    @Id
    private String paymentMethodId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String code;

    @Field(type = FieldType.Text)
    private String group;

    @Field(type = FieldType.Text)
    private String img;

    @Field(type = FieldType.Text)
    private GatewayEnum gateway;
}
