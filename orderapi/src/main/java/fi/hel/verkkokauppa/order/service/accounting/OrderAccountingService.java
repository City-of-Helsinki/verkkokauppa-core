package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.accounting.OrderAccountingDto;
import fi.hel.verkkokauppa.order.api.data.accounting.OrderItemAccountingDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderAccountingTransformer;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.repository.jpa.OrderAccountingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderAccountingService {

    private Logger log = LoggerFactory.getLogger(OrderAccountingService.class);

    @Autowired
    private OrderAccountingRepository orderAccountingRepository;

    public OrderAccounting createOrderAccounting(OrderAccountingDto orderAccountingDto) {
        OrderAccounting productAccountingEntity = new OrderAccountingTransformer().transformToEntity(orderAccountingDto);
        this.orderAccountingRepository.save(productAccountingEntity);
        return productAccountingEntity;
    }

    public OrderAccountingDto createOrderAccounting(String orderId, List<OrderItemAccountingDto> orderItemAccountings) {
        String createdAt = DateTimeUtil.getDate();
        OrderAccountingDto orderAccountingDto = new OrderAccountingDto(orderId, createdAt, orderItemAccountings);
        createOrderAccounting(orderAccountingDto);

        return orderAccountingDto;
    }

    public OrderAccounting getOrderAccounting(String orderId) {
        Optional<OrderAccounting> mapping = orderAccountingRepository.findById(orderId);

        if (mapping.isPresent())
            return mapping.get();

        log.warn("order accounting not found, orderId: " + orderId);
        return null;
    }

    public List<OrderAccounting> getOrderAccountings(List<String> orderIds) {
        List<OrderAccounting> accountings = orderAccountingRepository.findByOrderIdIn(orderIds);

        if (!accountings.isEmpty())
            return accountings;

        log.debug("order accountings not found, orderIds: " + orderIds);
        return new ArrayList<OrderAccounting>();
    }

}
