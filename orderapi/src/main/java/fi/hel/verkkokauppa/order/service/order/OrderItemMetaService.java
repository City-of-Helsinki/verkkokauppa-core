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


    public String addOrUpdateItemMeta(OrderItemMetaDto orderItemMetaDto) {
        String orderItemMetaId;
        List<OrderItemMeta> orderItemMetas = findByOrderItemId(orderItemMetaDto.getOrderItemId());
        for( OrderItemMeta meta: orderItemMetas ){
            if( meta.getKey().equals(orderItemMetaDto.getKey()) ){
                // key is same, update this
                meta.setValue(orderItemMetaDto.getValue());
                meta.setLabel(orderItemMetaDto.getLabel());
                meta.setVisibleInCheckout(orderItemMetaDto.getVisibleInCheckout());
                meta.setOrdinal(orderItemMetaDto.getOrdinal());

                orderItemMetaRepository.save(meta);
                log.debug("Updated orderItemMeta, orderItemMetaId: " + meta.getOrderItemMetaId());
                // no need to loop more or add meta. Just return
                return meta.getOrderItemMetaId();
            }
        }

        // no meta with key found, add it
        orderItemMetaId = addItemMeta(orderItemMetaDto);
        return orderItemMetaId;
    }

    public String addItemMeta(OrderItemMetaDto orderItemMetaDto) {
        String orderItemMetaId = UUIDGenerator.generateType4UUID().toString();

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
        return orderItemMetaId;
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

    public List<OrderItemMeta> findVisibleMetaByOrderItemId(String orderItemId) {
        List<OrderItemMeta> orderItemMetas = orderItemMetaRepository.findByOrderItemIdAndVisibleInCheckoutIsTrue(orderItemId);

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
