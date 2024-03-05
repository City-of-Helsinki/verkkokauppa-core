package fi.hel.verkkokauppa.order.api.admin;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.lock.ResourceLock;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class OrderAdminController {

    private Logger log = LoggerFactory.getLogger(OrderAdminController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private ResourceLock resourceLock;

    // when returning from payment, userId is not available at client
    @GetMapping(value = "/order-admin/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderAggregateDto> getOrder(@RequestParam(value = "orderId") String orderId) {
        try {
            return orderService.orderAggregateDto(orderId);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting order failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-order", "failed to get order with id [" + orderId + "]")
            );
        }
    }

    @PostMapping(value = "/order-admin/lock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> lockOrder(@RequestParam(value = "orderId") String orderId) {
        try {
            boolean res = resourceLock.obtain(orderId);
            return ResponseEntity.ok().body(res);
        } catch (Exception e) {
            log.error("locking order failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-lock-order", "failed to lock order with id [" + orderId + "]")
            );
        }
    }

    @PostMapping(value = "/order-admin/unlock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> unlockOrder(@RequestParam(value = "orderId") String orderId) {
        try {
            resourceLock.release(orderId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("unlocking order failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-unlock-order", "failed to unlock order with id [" + orderId + "]")
            );
        }
    }


    @GetMapping(value = "/order-admin/get-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderAggregateDto>> getOrders(@RequestParam(value = "userId") String userId) {
        try {
            List<OrderAggregateDto> orders = new ArrayList<>();
            for (Order order : orderService.findByUser(userId)) {
                orders.add(orderService.getOrderWithItems(order.getOrderId()));
            }
            return ResponseEntity.ok().body(orders);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-orders", "failed to get order for user id [" + userId + "]")
            );
        }
    }

}
