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
    private String material;
    private String materialDescription;
    private Integer quantity;
    private String unit;
    private String priceNet;
}
