package fi.hel.verkkokauppa.order.api.data.invoice;

import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderItemInvoicingDto {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private OrderItemInvoicingStatus status;
    private String orderItemId;
    private String orderId;
    private String orderIncrementId;
    private LocalDate invoicingDate;
    private String customerYid;
    private String customerOvt;
    private String customerName;
    private String customerAddress;
    private String customerPostcode;
    private String customerCity;
    private String material;
    private String orderType;
    private String salesOrg;
    private String salesOffice;
    private String materialDescription;
    private Integer quantity;
    private String unit;
    private String priceNet;

    private String internalOrder;

    private String profitCenter;

    private String project;

    private String operationArea;
}
