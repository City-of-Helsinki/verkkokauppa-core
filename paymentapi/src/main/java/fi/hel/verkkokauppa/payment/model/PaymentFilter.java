package fi.hel.verkkokauppa.payment.model;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "payment_filters")
@Data
public class PaymentFilter {
    @Id
    String filterId;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    LocalDateTime updatedAt;

    @Field(type = FieldType.Text)
    String namespace;

    @Field(type = FieldType.Text)
    String referenceId;

    @Field(type = FieldType.Text)
    ReferenceType referenceType;

    @Field(type = FieldType.Text)
    String type;

    @Field(type = FieldType.Text)
    String value;

    public void setUUID3FilterIdFromValueAndReferenceIdAndReferenceType(){
        String valueReferenceIdUUID3 = UUIDGenerator.generateType3UUIDString(getValue(), getReferenceId());
        String valueReferenceIdReferenceType = UUIDGenerator.generateType3UUIDString(valueReferenceIdUUID3,referenceType.getValue());
        this.setFilterId(valueReferenceIdReferenceType);
    }
}
