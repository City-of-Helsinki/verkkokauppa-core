package fi.hel.verkkokauppa.order.model.refund;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.refund.RefundItemDto;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "refunditems")
@Data
public class RefundItem {
  @Id
  String refundItemId;
  @Field(type = FieldType.Keyword)
  String refundId;
  @Field(type = FieldType.Keyword)
  String orderItemId;
  @Field(type = FieldType.Keyword)
  String orderId;

  @Field(type = FieldType.Keyword)
  String productId;
  @Field(type = FieldType.Text)
  String productName;
  @Field(type = FieldType.Text)
  String productLabel;
  @Field(type = FieldType.Text)
  String productDescription;
  @Field(type = FieldType.Text)
  Integer quantity;
  @Field(type = FieldType.Text)
  String unit;

  @Field(type = FieldType.Text)
  String rowPriceNet;
  @Field(type = FieldType.Text)
  String rowPriceVat;
  @Field(type = FieldType.Text)
  String rowPriceTotal;

  @Field(type = FieldType.Text)
  String vatPercentage;
  @Field(type = FieldType.Text)
  String priceNet;
  @Field(type = FieldType.Text)
  String priceVat;
  @Field(type = FieldType.Text)
  String priceGross;
  @Field(type = FieldType.Text)
  String originalPriceNet;
  @Field(type = FieldType.Text)
  String originalPriceVat;
  @Field(type = FieldType.Text)
  String originalPriceGross;

  public RefundItem(String refundId, RefundItemDto dto) {
    this.refundItemId = UUIDGenerator.generateType4UUID().toString();
    this.refundId = refundId;
    this.orderItemId = dto.getOrderItemId();
    this.orderId = dto.getOrderId();
    this.productId = dto.getProductId();
    this.productName = dto.getProductName();
    this.productLabel = dto.getProductLabel();
    this.productDescription = dto.getProductDescription();
    this.quantity = dto.getQuantity();
    this.unit = dto.getUnit();
    this.rowPriceNet = dto.getRowPriceNet();
    this.rowPriceVat = dto.getRowPriceVat();
    this.rowPriceTotal = dto.getRowPriceTotal();
    this.vatPercentage = dto.getVatPercentage();
    this.priceNet = dto.getPriceNet();
    this.priceVat = dto.getPriceVat();
    this.priceGross = dto.getPriceGross();
    this.originalPriceNet = dto.getOriginalPriceNet();
    this.originalPriceVat = dto.getOriginalPriceVat();
    this.originalPriceGross = dto.getOriginalPriceGross();
  }
}
