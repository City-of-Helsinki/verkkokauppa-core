package fi.hel.verkkokauppa.order.model.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "accountingsliprows")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingSlipRow {

    @Id
    private String accountingSlipRowId;

    @Field(type = FieldType.Keyword)
    private String accountingSlipId;

    @Field(type = FieldType.Text)
    private String taxCode;

    @Field(type = FieldType.Text)
    private String amountInDocumentCurrency;

    @Field(type = FieldType.Text)
    private String baseAmount;

    @Field(type = FieldType.Text)
    private String vatAmount;

    @Field(type = FieldType.Text)
    private String lineText;

    @Field(type = FieldType.Text)
    private String glAccount;

    @Field(type = FieldType.Text)
    private String profitCenter;

    @Field(type = FieldType.Text)
    private String orderItemNumber;

    @Field(type = FieldType.Text)
    private String wbsElement;

    @Field(type = FieldType.Text)
    private String functionalArea;

}
