package fi.hel.verkkokauppa.order.model.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "accountingexportdatas")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingSlipExportData {

    @Id
    private String timestamp;

    @Field(type = FieldType.Keyword)
    private String accountingSlipId;

    @Field(type = FieldType.Text)
    private String xml;

}
