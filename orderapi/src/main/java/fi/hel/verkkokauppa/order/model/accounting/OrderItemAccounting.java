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

@Document(indexName = "orderitemaccountings")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemAccounting {

    @Id
    private String orderItemId;

    @Field(type = FieldType.Text)
    private String orderId;

    @Field(type = FieldType.Text)
    private String priceGross;

    @Field(type = FieldType.Text)
    private String priceNet;

    @Field(type = FieldType.Text)
    private String priceVat;

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

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    LocalDateTime paidAt; // Timestamp when the transaction was paid

    @Field(type = FieldType.Text)
    private String merchantId;

    @Field(type = FieldType.Text)
    private String namespace;

    @Field(type = FieldType.Text)
    private String paytrailTransactionId;

}
