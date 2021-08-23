package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.order.api.data.CustomerDto;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.TotalsDto;
import fi.hel.verkkokauppa.order.logic.OrderTypeLogic;
import fi.hel.verkkokauppa.order.service.CommonBeanValidationService;

import java.math.BigDecimal;

import javax.validation.ConstraintViolationException;

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
import fi.hel.verkkokauppa.order.model.OrderStatus;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.order.OrderItemMetaService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;

@RestController
public class OrderController {

	private Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemMetaService orderItemMetaService;

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

    @GetMapping(value = "/order/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> confirmOrder(@RequestParam(value = "orderId") String orderId) {
        try {
            Order order = orderService.findById(orderId);

            validateCustomerData(order.getCustomerFirstName(), order.getCustomerLastName(), order.getCustomerEmail(), order.getCustomerPhone());
            validateOrderTotalsExist(order);

            orderService.confirm(order);
            return orderAggregateDto(orderId);

        } catch (ConstraintViolationException cve) {
            log.warn("confirming invalid order rejected, orderId: " + orderId, cve);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception e) {
            log.error("confirming order failed, orderId: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/order/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> cancelOrder(@RequestParam(value = "orderId") String orderId) {
        try {
            Order order = orderService.findById(orderId);
            orderService.cancel(order);
            return orderAggregateDto(orderId);

        } catch (Exception e) {
            log.error("canceling order failed, orderId: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/order/setCustomer", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> setCustomer(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "customerFirstName") String customerFirstName,
            @RequestParam(value = "customerLastName") String customerLastName, @RequestParam(value = "customerEmail") String customerEmail, @RequestParam(value = "customerPhone") String customerPhone) {
        try {
            Order order = orderService.findById(orderId);
            if (!changesToOrderAllowed(order))
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                
            CustomerDto customerDto = validateCustomerData(customerFirstName, customerLastName, customerEmail, customerPhone);
            orderService.setCustomer(order, customerDto);
            return orderAggregateDto(orderId);

        } catch (Exception e) {
            log.error("setting order customer failed, orderId: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/order/setItems", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> setItems(@RequestParam(value = "orderId") String orderId, @RequestBody OrderAggregateDto dto) {
        try {
            Order order = orderService.findById(orderId);
            if (!changesToOrderAllowed(order))
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

            if (dto != null && dto.getItems() != null) {
                dto.getItems().stream().forEach(item -> {
                    String orderItemId = orderItemService.addItem(
                            orderId,
                            item.getProductId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getUnit(),
                            item.getRowPriceNet(),
                            item.getRowPriceVat(),
                            item.getRowPriceTotal(),
                            item.getVatPercentage(),
                            item.getPriceNet(),
                            item.getPriceVat(),
                            item.getPriceGross()
                    );

                    if (item.getMeta() != null) {
                        item.getMeta().stream().forEach(meta -> {
                            meta.setOrderItemId(orderItemId);
                            meta.setOrderId(orderId);
                            orderItemMetaService.addItemMeta(meta);
                        });
                    }
                });
            }

            String orderType = orderTypeLogic.decideOrderTypeBasedOnItems(dto.getItems());
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
            Order order = orderService.findById(orderId);
            if (!changesToOrderAllowed(order))
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

            orderService.setTotals(order, priceNet, priceVat, priceTotal);
            return orderAggregateDto(orderId);

        } catch (Exception e) {
            log.error("setting order totals failed, orderId: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
	}

    @PostMapping(value = "/order/createWithItems", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderAggregateDto> createWithItems(@RequestBody OrderAggregateDto orderAggregateDto) {
        try {
            OrderDto orderDto = orderAggregateDto.getOrder();
            Order order = orderService.createByParams(orderDto.getNamespace(), orderDto.getUser());
            String orderId = order.getOrderId();

            String orderType = orderTypeLogic.decideOrderTypeBasedOnItems(orderAggregateDto.getItems());
            orderService.setType(order, orderType);

            CustomerDto customerDto = validateCustomerData(orderDto.getCustomerFirstName(), orderDto.getCustomerLastName(), orderDto.getCustomerEmail(), orderDto.getCustomerPhone());
            orderService.setCustomer(order, customerDto);

            setItems(orderId, orderAggregateDto);
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

    private CustomerDto validateCustomerData(String customerFirstName, String customerLastName, String customerEmail, String customerPhone) {
        CustomerDto customerDto = CustomerDto.builder()
        .customerLastName(customerLastName)
        .customerFirstName(customerFirstName)
        .customerEmail(customerEmail)
        .customerPhone(customerPhone)
        .build();

        commonBeanValidationService.validateInput(customerDto);
        return customerDto;
    }

    private TotalsDto validateOrderTotalsExist(Order order) {
        TotalsDto totalsDto = TotalsDto.builder()
        .priceNet(new BigDecimal(order.getPriceNet()))
        .priceVat(new BigDecimal(order.getPriceVat()))
        .priceTotal(new BigDecimal(order.getPriceTotal()))
        .build();

        commonBeanValidationService.validateInput(totalsDto);
        return totalsDto;
    }

    private boolean changesToOrderAllowed(Order order) {
        boolean changesToOrderAllowed = (order != null && OrderStatus.DRAFT.equals(order.getStatus()));
        log.debug("changesToOrderAllowed order: " + order.getOrderId() + " allowed: " + changesToOrderAllowed);
        return changesToOrderAllowed;
    }

    
}
