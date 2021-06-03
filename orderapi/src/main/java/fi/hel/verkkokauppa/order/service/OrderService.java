package fi.hel.verkkokauppa.order.service;

import java.util.List;
import java.util.Optional;

import fi.hel.verkkokauppa.util.DateTimeUtil;
import fi.hel.verkkokauppa.util.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderStatus;


@Component
public class OrderService {
        
    private Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    public String generateOrderId(String namespace, String user, String timestamp) {
        String whoseOrder = UUIDGenerator.generateType3UUIDString(namespace, user);
        String orderId = UUIDGenerator.generateType3UUIDString(whoseOrder, timestamp);
        return orderId;
    }

    public Order createByParams(String namespace, String user) {
        String createdAt = DateTimeUtil.getDateTime();
        String orderId = generateOrderId(namespace, user, createdAt);
        Order order = new Order(orderId, namespace, user, createdAt);
        orderRepository.save(order);
        log.debug("created new order, orderId: " + orderId);
        return order;
    }

    public Order findById(String orderId) {
        Optional<Order> mapping = orderRepository.findById(orderId);
        
        if (mapping.isPresent())
            return mapping.get();

        log.debug("order not found, orderId: " + orderId);
        return null;
    }

    public Order findByNamespaceAndUser(String namespace, String user) {
        List<Order> matchingOrders = orderRepository.findByNamespaceAndUser(namespace, user);

        if (matchingOrders.size() > 0)
            return matchingOrders.get(0);

        log.debug("order not found, namespace: " + namespace + " user: " + user);
        return null;
    }

    public void setCustomer(String orderId, String customerFirstName, String customerLastName, String customerEmail) {
        Order order = findById(orderId);
        order.setCustomerFirstName(customerFirstName);
        order.setCustomerLastName(customerLastName);
        order.setCustomerEmail(customerEmail);
        orderRepository.save(order);
        log.debug("saved order customer details, orderId: " + orderId);
    }

    public void cancel(String orderId) {
        Order order = findById(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.debug("canceled order, orderId: " + orderId);
    }
    
}
