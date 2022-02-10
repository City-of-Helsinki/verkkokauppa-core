package fi.hel.verkkokauppa.order.service.subscription;

import java.util.List;

import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.api.data.transformer.SubscriptionItemMetaTransformer;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionItemMeta;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionItemMetaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionItemMetaService {
    private Logger log = LoggerFactory.getLogger(SubscriptionItemMetaService.class);

    @Autowired
    private SubscriptionItemMetaRepository subscriptionItemMetaRepository;

    @Autowired
    private SubscriptionItemMetaTransformer subscriptionItemMetaTransformer;

    public OrderItemMetaDto addItemMeta(OrderItemMetaDto orderMeta, String subscriptionId) {
        SubscriptionItemMeta subscriptionMeta = subscriptionItemMetaTransformer.transformToEntity(orderMeta);
        subscriptionMeta.setSubscriptionId(subscriptionId);
        subscriptionMeta = subscriptionItemMetaRepository.save(subscriptionMeta);
        log.debug("created new subscriptionItemMeta " + subscriptionMeta.getOrderItemMetaId() + " from createdSubscriptionItemMeta: " + orderMeta.getOrderItemMetaId());
        return subscriptionItemMetaTransformer.transformToDto(subscriptionMeta);
    }

    public List<SubscriptionItemMeta> removeItemMetas(String subscriptionId, String orderItemId) {
        List<SubscriptionItemMeta> metas = subscriptionItemMetaRepository.findBySubscriptionIdAndOrderItemId(subscriptionId, orderItemId);
        metas.forEach(meta -> {
            subscriptionItemMetaRepository.deleteById(meta.getOrderItemMetaId());
        });
        return metas;
    }
}
