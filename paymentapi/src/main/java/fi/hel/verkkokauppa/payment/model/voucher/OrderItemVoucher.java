package fi.hel.verkkokauppa.payment.model.voucher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "orderitemvouchers")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemVoucher {

    // Technical fields
    @Id
    String orderItemVoucherId;
    @Field(
            type = FieldType.Date,
            format = {},
            pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'"
    )
    LocalDateTime createdAt;
    @LastModifiedDate
    @Field(
            type = FieldType.Date,
            format = {},
            pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'"
    )
    LocalDateTime updatedAt;
    @Field(type = FieldType.Keyword)
    String orderItemId;
    @Field(type = FieldType.Keyword)
    String orderId;
    @Field(type = FieldType.Keyword)
    String merchantId;
    @Field(type = FieldType.Keyword)
    String namespace;

    // Token/Voucher fields
    @Field(type = FieldType.Keyword)
    String tokenId;
    @Field(type = FieldType.Keyword)
    String tokenName; // actual barcode/QR code
    @Field(type = FieldType.Text)
    String tokenQRCodeUrl; // url to get the QR code file to be added to order confirmation

    @Field(type = FieldType.Text)
    Integer quantityLeft;
    @Field(type = FieldType.Text)
    String amountLeft;
    @Field(
            type = FieldType.Date,
            format = {},
            pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'"
    )
    LocalDateTime expirationDate;


}
