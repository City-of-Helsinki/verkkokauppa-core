package fi.hel.verkkokauppa.order.model.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "accountingexportdatas")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingExportData {

    @Id
    private String accountingSlipId;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime timestamp;

    @Field(type = FieldType.Text)
    private String xml;

    @Field(type = FieldType.Text)
    private String exported;

}
