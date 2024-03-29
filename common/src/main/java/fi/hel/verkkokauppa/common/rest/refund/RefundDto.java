package fi.hel.verkkokauppa.common.rest.refund;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundDto {

    private String refundId;
    private String orderId;
    private String namespace;
    private String user;
    private String createdAt;
    private String status;

    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String customerPhone;

    private String refundReason;

    private String priceNet;
    private String priceVat;
    private String priceTotal;
    private LocalDate accounted;
}