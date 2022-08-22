package fi.hel.verkkokauppa.order.model.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

@Document(indexName = "accountingslips")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingSlip {

    @Id
    private String accountingSlipId;

    @Field(type = FieldType.Text)
    private String companyCode;

    @Field(type = FieldType.Text)
    private String documentType;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate documentDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate postingDate;

    @Field(type = FieldType.Text)
    private String reference;

    @Field(type = FieldType.Text)
    private String headerText;

    @Field(type = FieldType.Text)
    private String currencyCode;

}
