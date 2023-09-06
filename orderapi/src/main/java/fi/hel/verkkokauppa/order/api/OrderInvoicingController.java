package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.order.api.data.invoice.OrderItemInvoicingDto;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import fi.hel.verkkokauppa.order.service.invoice.OrderItemInvoicingService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class OrderInvoicingController {
    private Logger log = LoggerFactory.getLogger(OrderInvoicingController.class);

    @Autowired
    private OrderItemInvoicingService orderItemInvoicingService;

    @Autowired
    private OrderItemService orderItemService;

    @PostMapping(value = "/order/invoicing/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderItemInvoicingDto>> createOrderInvoicing(@RequestBody List<OrderItemInvoicingDto> dtos) {
        List<OrderItemInvoicingDto> res = new ArrayList<>();
        for (OrderItemInvoicingDto dto : dtos) {
            res.add(this.orderItemInvoicingService.create(dto));
            orderItemService.setInvoicingStatus(dto.getOrderItemId(), OrderItemInvoicingStatus.CREATED);
        }
        return ResponseEntity.ok().body(res);
    }
}
