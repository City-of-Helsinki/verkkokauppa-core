package fi.hel.verkkokauppa.order.api;

import java.util.List;

import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformerUtils;
import fi.hel.verkkokauppa.order.logic.OrderTypeLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

	@Autowired
	private OrderTypeLogic orderTypeLogic;

    @GetMapping("/order/create")
	public ResponseEntity<Order> createOrder(@RequestParam(value = "namespace") String namespace,
                                             @RequestParam(value = "user") String user) {
        final Order order = orderService.createByParams(namespace, user);
        ResponseEntity<Order> response = ResponseEntity.ok().body(order);

		return response;
	}

    @GetMapping("/order/get")
	public OrderAggregateDto getOrder(@RequestParam(value = "orderId") String orderId) {
        return getOrderWithItems(orderId);
	}

    private OrderAggregateDto getOrderWithItems(final String orderId) {
        OrderAggregateDto orderAggregateDto = orderService.getOrderWithItems(orderId);
        return orderAggregateDto;
    }

    @GetMapping("/order/cancel")
	public OrderAggregateDto cancelOrder(@RequestParam(value = "orderId") String orderId) {
		orderService.cancel(orderId);
        return getOrderWithItems(orderId);
    }

    @PostMapping("/order/setCustomer")
	public OrderAggregateDto setCustomer(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "customerFirstName") String customerFirstName,
            @RequestParam(value = "customerLastName") String customerLastName, @RequestParam(value = "customerEmail") String customerEmail) {
		orderService.setCustomer(orderId, customerFirstName, customerLastName, customerEmail);
        return getOrderWithItems(orderId);
	}

    @PostMapping("/order/setItems")
	public OrderAggregateDto setItems(@RequestParam(value = "orderId") String orderId, @RequestBody OrderAggregateDto dto) {
        if (dto != null && dto.getOrderItemDtos() != null) {
            dto.getOrderItemDtos().stream().forEach(item -> {
                orderItemService.addItem(orderId, item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnit(), 
                    item.getRowPriceNet(), item.getRowPriceVat(), item.getRowPriceTotal());}
                );
        }
        // TODO: what if order items change and is subscription type?

        return getOrderWithItems(orderId);
	}

    @PostMapping("/order/createWithItems")
    public OrderAggregateDto createWithItems(@RequestBody OrderAggregateDto orderAggregateDto) {
        OrderDto orderDto = orderAggregateDto.getOrderDto();
        Order order = orderService.createByParams(orderDto.getNamespace(), orderDto.getUser());

		// TODO: refactor this code and move this to service!
		orderTypeLogic.setOrderType(order, orderAggregateDto.getOrderItemDtos());

        orderService.setCustomer(order.getOrderId(), orderDto.getCustomerFirstName(), orderDto.getCustomerLastName(), orderDto.getCustomerEmail());
        setItems(order.getOrderId(), orderAggregateDto);

        return getOrderWithItems(order.getOrderId());
    }

}
