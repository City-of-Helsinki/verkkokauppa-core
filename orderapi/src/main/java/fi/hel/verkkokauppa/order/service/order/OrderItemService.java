package fi.hel.verkkokauppa.order.service.order;

import java.util.List;
import java.util.Optional;

import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.utils.UUIDGenerator;
@Component
public class OrderItemService {
        
    private Logger log = LoggerFactory.getLogger(OrderItemService.class);

    @Autowired
    private OrderItemRepository orderItemRepository;


    public void addItem(String orderId, String productId, String productName, Integer quantity, String unit, String rowPriceNet, String rowPriceVat, String rowPriceTotal) {
        String orderItemId = UUIDGenerator.generateType3UUIDString(orderId, productId);
        OrderItem orderItem = new OrderItem(orderItemId, orderId, productId, productName, quantity, unit, rowPriceNet, rowPriceVat, rowPriceTotal);
        orderItemRepository.save(orderItem);
        log.debug("created new orderItem, orderItemId: " + orderItemId);
    }

    public void removeItem(String orderId, String productId) {
        String orderItemId = UUIDGenerator.generateType3UUIDString(orderId, productId);
        orderItemRepository.deleteById(orderItemId);
        log.debug("deleted orderItem, orderItemId: " + orderItemId);
    }

    public OrderItem findById(String orderItemId) {
        Optional<OrderItem> mapping = orderItemRepository.findById(orderItemId);
        
        if (mapping.isPresent())
            return mapping.get();

        log.debug("orderItem not found, orderItemId: " + orderItemId);
        return null;
    }

    public List<OrderItem> findByOrderId(String orderId) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        if (orderItems.size() > 0)
            return orderItems;

        log.debug("orderItems not found, orderId: " + orderId);
        return null;
    }

}
