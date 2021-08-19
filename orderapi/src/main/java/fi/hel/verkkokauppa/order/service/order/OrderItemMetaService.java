package fi.hel.verkkokauppa.order.service.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemMetaRepository;

@Component
public class OrderItemMetaService {
        
    private Logger log = LoggerFactory.getLogger(OrderItemMetaService.class);

    @Autowired
    private OrderItemMetaRepository orderItemMetaRepository;


    public void addItemMeta(OrderItemMetaDto orderItemMetaDto) {
        String orderItemMetaId = UUIDGenerator.generateType3UUIDString(orderItemMetaDto.getOrderItemId(), orderItemMetaDto.getKey());

        OrderItemMeta orderItem = new OrderItemMeta(
            orderItemMetaId, 
            orderItemMetaDto.getOrderItemId(),
            orderItemMetaDto.getOrderId(), 
            orderItemMetaDto.getKey(), 
            orderItemMetaDto.getValue(), 
            orderItemMetaDto.getLabel(), 
            orderItemMetaDto.getVisibleInCheckout(), 
            orderItemMetaDto.getOrdinal()
        );

        orderItemMetaRepository.save(orderItem);
        log.debug("created new orderItemMeta, orderItemMetaId: " + orderItemMetaId);
    }

    public void removeItemMeta(String orderItemMetaId) {
        orderItemMetaRepository.deleteById(orderItemMetaId);
        log.debug("deleted orderItemMeta, orderItemMetaId: " + orderItemMetaId);
    }

    public OrderItemMeta findById(String orderItemMetaId) {
        Optional<OrderItemMeta> mapping = orderItemMetaRepository.findById(orderItemMetaId);
        
        if (mapping.isPresent())
            return mapping.get();

        log.debug("orderItemMeta not found, orderItemMetaId: " + orderItemMetaId);
        return null;
    }

    public List<OrderItemMeta> findByOrderItemId(String orderItemId) {
        List<OrderItemMeta> orderItemMetas = orderItemMetaRepository.findByOrderItemId(orderItemId);

        if (orderItemMetas.size() > 0)
            return orderItemMetas;

        log.debug("orderItemMetas not found, orderItemId: " + orderItemId);
        return new ArrayList<OrderItemMeta>();
    }

    public List<OrderItemMeta> findByOrderId(String orderId) {
        List<OrderItemMeta> orderItemMetas = orderItemMetaRepository.findByOrderId(orderId);

        if (orderItemMetas.size() > 0)
            return orderItemMetas;

        log.debug("orderItemMetas not found, orderId: " + orderId);
        return new ArrayList<OrderItemMeta>();
    }

}
