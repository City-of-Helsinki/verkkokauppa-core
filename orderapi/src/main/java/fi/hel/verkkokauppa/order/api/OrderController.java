package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.order.api.data.CustomerDto;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.logic.OrderTypeLogic;
import fi.hel.verkkokauppa.order.service.CommonBeanValidationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;

@RestController
public class OrderController {

	private Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

	@Autowired
	private OrderTypeLogic orderTypeLogic;

	@Autowired
    private CommonBeanValidationService commonBeanValidationService;

    @GetMapping(value = "/order/create", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> createOrder(@RequestParam(value = "namespace") String namespace,
                                             @RequestParam(value = "user") String user) {
        try {
            Order order = orderService.createByParams(namespace, user);
            String orderId = order.getOrderId();
            return orderAggregateDto(orderId);

        } catch (Exception e) {
            log.error("creating order failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
	}

    @GetMapping(value = "/order/get", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> getOrder(@RequestParam(value = "orderId") String orderId) {
        try {
            return orderAggregateDto(orderId);

        } catch (Exception e) {
            log.error("getting order failed, orderId: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/order/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> cancelOrder(@RequestParam(value = "orderId") String orderId) {
        try {
            orderService.cancel(orderId);
            return orderAggregateDto(orderId);

        } catch (Exception e) {
            log.error("canceling order failed, orderId: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/order/setCustomer", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> setCustomer(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "customerFirstName") String customerFirstName,
            @RequestParam(value = "customerLastName") String customerLastName, @RequestParam(value = "customerEmail") String customerEmail) {
        try {
            CustomerDto customerDto = CustomerDto.builder()
                    .customerLastName(customerLastName)
                    .customerFirstName(customerFirstName)
                    .customerEmail(customerEmail)
                    .build();

            commonBeanValidationService.validateInput(customerDto);

            orderService.setCustomer(orderId, customerFirstName, customerLastName, customerEmail);
            return orderAggregateDto(orderId);

        } catch (Exception e) {
            log.error("setting order customer failed, orderId: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/order/setItems", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> setItems(@RequestParam(value = "orderId") String orderId, @RequestBody OrderAggregateDto dto) {
        try {
            if (dto != null && dto.getOrderItemDtos() != null) {
                dto.getOrderItemDtos().stream().forEach(item -> {
                    orderItemService.addItem(orderId, item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnit(), 
                        item.getRowPriceNet(), item.getRowPriceVat(), item.getRowPriceTotal());}
                    );
            }

            Order order = orderService.findById(orderId);
            String orderType = orderTypeLogic.decideOrderTypeBasedOnItems(dto.getOrderItemDtos());
            orderService.setType(order, orderType);

            return orderAggregateDto(orderId);

        } catch (Exception e) {
            log.error("setting order items failed, orderId: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
	}

    @PostMapping(value = "/order/setTotals", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> setTotals(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "priceNet") String priceNet, 
            @RequestParam(value = "priceVat") String priceVat, @RequestParam(value = "priceTotal") String priceTotal) {
        try {
            orderService.setTotals(orderId, priceNet, priceVat, priceTotal);
            return orderAggregateDto(orderId);

        } catch (Exception e) {
            log.error("setting order totals failed, orderId: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
	}

    @PostMapping(value = "/order/createWithItems", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderAggregateDto> createWithItems(@RequestBody OrderAggregateDto orderAggregateDto) {
        try {
            OrderDto orderDto = orderAggregateDto.getOrderDto();
            Order order = orderService.createByParams(orderDto.getNamespace(), orderDto.getUser());
            String orderId = order.getOrderId();

            orderService.setCustomer(order, orderDto.getCustomerFirstName(), orderDto.getCustomerLastName(), orderDto.getCustomerEmail());
            setItems(order.getOrderId(), orderAggregateDto);
            orderService.setTotals(order, orderDto.getPriceNet(), orderDto.getPriceVat(), orderDto.getPriceTotal());

            return orderAggregateDto(orderId);

        } catch (Exception e) {
            log.error("creating order with items failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private OrderAggregateDto getOrderWithItems(String orderId) {
        OrderAggregateDto orderAggregateDto = orderService.getOrderWithItems(orderId);
        return orderAggregateDto;
    }

    private ResponseEntity<OrderAggregateDto> orderAggregateDto(String orderId) {
        return ResponseEntity.ok().body(getOrderWithItems(orderId));
    }

}
