package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.api.data.accounting.CreateOrderAccountingRequestDto;
import fi.hel.verkkokauppa.order.api.data.accounting.OrderAccountingDto;
import fi.hel.verkkokauppa.order.api.data.accounting.OrderItemAccountingDto;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.service.accounting.OrderAccountingService;
import fi.hel.verkkokauppa.order.service.accounting.OrderItemAccountingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderAccountingController {

    private Logger log = LoggerFactory.getLogger(OrderAccountingController.class);

    @Autowired
    private OrderAccountingService orderAccountingService;

    @Autowired
    private OrderItemAccountingService orderItemAccountingService;

    @PostMapping(value = "/order/accounting/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderAccountingDto> createOrderAccounting(@RequestBody CreateOrderAccountingRequestDto request) {
        try {
            String orderId = request.getOrderId();
            OrderAccounting orderAccounting = orderAccountingService.getOrderAccounting(orderId);

            if (orderAccounting != null) {
                log.info("Accounting for order already created");
                return ResponseEntity.ok().build();
            }
            List<OrderItemAccountingDto> orderItemAccountings = orderItemAccountingService.createOrderItemAccountings(request);
            OrderAccountingDto orderAccountingDto = orderAccountingService.createOrderAccounting(orderId, request.getNamespace(), orderItemAccountings);

            return ResponseEntity.ok().body(orderAccountingDto);

        } catch (Exception e) {
            log.error("creating order accounting failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-order-accounting", "failed to create order accounting")
            );
        }
    }

}
