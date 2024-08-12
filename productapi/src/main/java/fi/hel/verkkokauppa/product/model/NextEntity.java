package fi.hel.verkkokauppa.product.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
public class NextEntity {
    @Id
    private String productId;

    @Field(type = FieldType.Text)
    private String companyCode;

    @Field(type = FieldType.Text)
    private String mainLedgerAccount;

    @Field(type = FieldType.Text)
    private String vatCode;

    @Field(type = FieldType.Text)
    private String internalOrder;

    @Field(type = FieldType.Text)
    private String profitCenter;

    @Field(type = FieldType.Text)
    private String balanceProfitCenter;

    @Field(type = FieldType.Text)
    private String project;

    @Field(type = FieldType.Text)
    private String operationArea;
}
