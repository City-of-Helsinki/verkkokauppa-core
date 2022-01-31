package fi.hel.verkkokauppa.order.service.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;

@Component
public class OrderItemService {
        
    private Logger log = LoggerFactory.getLogger(OrderItemService.class);

    @Autowired
    private OrderItemRepository orderItemRepository;

    public String addItem(String orderId, String productId, String productName, String productLabel, String productDescription, Integer quantity, String unit,
                          String rowPriceNet, String rowPriceVat, String rowPriceTotal, String vatPercentage, String priceNet, String priceVat, String priceGross,
                          String periodUnit, Long periodFrequency, Integer periodCount, LocalDateTime billingStartDate, LocalDateTime startDate) {
        String orderItemId = UUIDGenerator.generateType4UUID().toString();
        OrderItem orderItem = new OrderItem(
                orderItemId,
                orderId,
                productId,
                productName,
                productLabel,
                productDescription,
                quantity,
                unit,
                rowPriceNet,
                rowPriceVat,
                rowPriceTotal,
                vatPercentage,
                priceNet,
                priceVat,
                priceGross,
                periodUnit,
                periodFrequency,
                periodCount,
                billingStartDate,
                startDate
        );
        orderItemRepository.save(orderItem);

        log.debug("created new orderItem, orderItemId: " + orderItemId);
        return orderItemId;
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
        return new ArrayList<OrderItem>();
    }

}
