package fi.hel.verkkokauppa.order.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.service.OrderService;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;


    @GetMapping("/order/create")
	public Order createOrder(@RequestParam(value = "namespace") String namespace, 
            @RequestParam(value = "user") String user) {
		return orderService.createByParams(namespace, user);
	}

    @GetMapping("/order/get")
	public Order getOrder(@RequestParam(value = "orderId") String orderId) {
		return orderService.findById(orderId);
	}

}
