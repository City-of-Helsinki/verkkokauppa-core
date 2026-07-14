package fi.hel.verkkokauppa.product.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Document(indexName = "accounting")
@Data
public class ProductAccounting extends BaseModel {
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

    @Field(type = FieldType.Text)
    private String namespace;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant activeFrom;

    @Field(type = FieldType.Object)
    private NextEntity nextEntity;

    public LocalDateTime getActiveFrom() {
        return LocalDateTime.ofInstant(this.activeFrom, ZoneOffset.UTC);
    }

    public void setActiveFrom(LocalDateTime activeFrom) {
        this.activeFrom = activeFrom.atZone(ZoneOffset.UTC).toInstant();
    }
}
