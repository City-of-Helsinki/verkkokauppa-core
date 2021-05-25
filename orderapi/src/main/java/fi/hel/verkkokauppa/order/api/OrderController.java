package fi.hel.verkkokauppa.order.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.service.OrderService;
import fi.hel.verkkokauppa.order.service.OrderItemService;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;


    @GetMapping("/order/create")
	public Order createOrder(@RequestParam(value = "namespace") String namespace, 
            @RequestParam(value = "user") String user) {
		return orderService.createByParams(namespace, user);
	}
    
	private OrderDto getOrderWithItems(@RequestParam(value = "orderId") String orderId) {
		Order order = orderService.findById(orderId);
		List<OrderItem> items = orderItemService.findByOrderId(orderId);

        return new OrderDto(order, items);
	}

    @GetMapping("/order/get")
	public OrderDto getOrder(@RequestParam(value = "orderId") String orderId) {
        return getOrderWithItems(orderId);
	}

    @GetMapping("/order/cancel")
	public OrderDto cancelOrder(@RequestParam(value = "orderId") String orderId) {
		orderService.cancel(orderId);
        return getOrderWithItems(orderId);
    }

    @PostMapping("/order/setCustomer")
	public OrderDto setCustomer(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "customerFirstName") String customerFirstName, 
            @RequestParam(value = "customerLastName") String customerLastName, @RequestParam(value = "customerEmail") String customerEmail) {
		orderService.setCustomer(orderId, customerFirstName, customerLastName, customerEmail);
        return getOrderWithItems(orderId);
	}

    @PostMapping("/order/setItems")
	public OrderDto setItems(@RequestParam(value = "orderId") String orderId, @RequestBody OrderDto dto) {
        if (dto != null && dto.getItems() != null) {
            dto.getItems().stream().forEach(item -> {
                orderItemService.addItem(orderId, item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnit(), 
                    item.getRowPriceNet(), item.getRowPriceVat(), item.getRowPriceTotal());}
                );
        }

        return getOrderWithItems(orderId);
	}

    @PostMapping("/order/createWithItems")
    public OrderDto createWithItems(@RequestBody OrderDto dto) {
        Order orderIn = dto.getOrder();
        Order order = orderService.createByParams(orderIn.getNamespace(), orderIn.getUser());
        orderService.setCustomer(order.getOrderId(), orderIn.getCustomerFirstName(), orderIn.getCustomerLastName(), orderIn.getCustomerEmail());
        setItems(order.getOrderId(), dto);

        return getOrderWithItems(order.getOrderId());
    }

}
