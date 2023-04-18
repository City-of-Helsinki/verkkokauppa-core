package fi.hel.verkkokauppa.payment.model.paytrail.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.helsinki.paytrail.constants.PaymentMethodGroup;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaytrailPaymentProviderModel {
    @Field(type = FieldType.Keyword)
    private String url;

    @Field(type = FieldType.Keyword)
    private String icon;

    @Field(type = FieldType.Keyword)
    private String svg;

    @Field(type = FieldType.Auto)
    private PaymentMethodGroup group;

    @Field(type = FieldType.Keyword)
    private String name;

    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Object)
    private List<FormFieldModel> parameters;
}
