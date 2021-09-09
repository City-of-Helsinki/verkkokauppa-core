package fi.hel.verkkokauppa.order.model.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "accountingslips")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingSlip {

    @Id
    private String accountingSlipId;

    @Field(type = FieldType.Text)
    private String documentDate;

    @Field(type = FieldType.Text)
    private String companyCode;

    @Field(type = FieldType.Text)
    private String documentType;

    @Field(type = FieldType.Text)
    private String postingDate;

    @Field(type = FieldType.Text)
    private String reference;

    @Field(type = FieldType.Text)
    private String headerText;

    @Field(type = FieldType.Text)
    private String currencyCode;

}
