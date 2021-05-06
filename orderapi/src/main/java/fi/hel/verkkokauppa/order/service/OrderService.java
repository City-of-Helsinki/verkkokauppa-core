package fi.hel.verkkokauppa.order.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.utils.UUIDGenerator;

@Component
public class OrderService {
        
    private Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;


    public Order createByParams(String namespace, String user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String createdAt = LocalDate.now().format(formatter);

        String whoseOrder = UUIDGenerator.generateType3UUIDString(namespace, user);
        String orderId = UUIDGenerator.generateType3UUIDString(whoseOrder, createdAt);

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
    
}
