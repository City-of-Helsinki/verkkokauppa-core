package fi.hel.verkkokauppa.order.model.refund;

import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "refunds")
@Data
public class Refund implements Persistable<String> {
  @Id
  String refundId;

  @Field(type = FieldType.Text)
  String orderId;

  @Field(type = FieldType.Keyword)
  String namespace;

  @Field(type = FieldType.Keyword)
  String user;

  @Field(type = FieldType.Date, format = DateFormat.date_time)
  LocalDateTime createdAt;

  @Field(type = FieldType.Text)
  String status;

  @Field(type = FieldType.Text)
  String customerFirstName;

  @Field(type = FieldType.Text)
  String customerLastName;

  @Field(type = FieldType.Text)
  String customerEmail;

  @Field(type = FieldType.Text)
  String customerPhone;

  @Field(type = FieldType.Text)
  String refundReason;

  @Field(type = FieldType.Text)
  String priceNet;

  @Field(type = FieldType.Text)
  String priceVat;

  @Field(type = FieldType.Text)
  String priceTotal;

  @LastModifiedDate
  @Field(type = FieldType.Date, format = DateFormat.date_time)
  LocalDateTime updatedAt;

  public String getId() {
    return this.refundId;
  }

  @Override
  public boolean isNew() {
    return updatedAt == null;
  }

  public Refund() {}

  public static Refund fromRefundDto(RefundDto dto) {
    Refund refund = new Refund();
    refund.setStatus(RefundStatus.DRAFT);
    refund.setRefundId(UUIDGenerator.generateType4UUID().toString());
    refund.setCreatedAt(DateTimeUtil.getFormattedDateTime());
    refund.setOrderId(dto.getOrderId());
    refund.setNamespace(dto.getNamespace());
    refund.setUser(dto.getUser());
    refund.setCustomerFirstName(dto.getCustomerFirstName());
    refund.setCustomerLastName(dto.getCustomerLastName());
    refund.setCustomerEmail(dto.getCustomerEmail());
    refund.setCustomerPhone(dto.getCustomerPhone());
    refund.setRefundReason(dto.getRefundReason());
    refund.setPriceNet(dto.getPriceNet());
    refund.setPriceVat(dto.getPriceVat());
    refund.setPriceTotal(dto.getPriceTotal());
    return refund;
  }

}
