package fi.hel.verkkokauppa.order.model.refund;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.refund.RefundDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "refunds")
@Setter
@Getter
public class Refund {
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

  public Refund(RefundDto dto) {
    this.status = RefundStatus.DRAFT;
    this.refundId = UUIDGenerator.generateType4UUID().toString();
    this.createdAt = DateTimeUtil.getFormattedDateTime();
    this.orderId = dto.getOrderId();
    this.namespace = dto.getNamespace();
    this.user = dto.getUser();
    this.customerFirstName = dto.getCustomerFirstName();
    this.customerLastName = dto.getCustomerLastName();
    this.customerEmail = dto.getCustomerEmail();
    this.customerPhone = dto.getCustomerPhone();
    this.refundReason = dto.getRefundReason();
  }
}
