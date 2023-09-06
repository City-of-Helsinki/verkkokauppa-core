package fi.hel.verkkokauppa.order.service.invoice;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.invoice.OrderItemInvoicingDto;
import fi.hel.verkkokauppa.order.mapper.OrderItemInvoicingMapper;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicing;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemInvoicingRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderItemInvoicingService {
    final OrderItemInvoicingRepository orderItemInvoicingRepository;

    final OrderItemInvoicingMapper orderItemInvoicingMapper;

    final OrderItemRepository orderItemRepository;

    @Autowired
    public OrderItemInvoicingService(OrderItemInvoicingRepository orderItemInvoicingRepository, OrderItemInvoicingMapper orderItemInvoicingMapper, OrderItemRepository orderItemRepository) {
        this.orderItemInvoicingRepository = orderItemInvoicingRepository;
        this.orderItemInvoicingMapper = orderItemInvoicingMapper;
        this.orderItemRepository = orderItemRepository;
    }

    public OrderItemInvoicing save(OrderItemInvoicing orderItemInvoicing) {
        orderItemInvoicing.setUpdatedAt(DateTimeUtil.getFormattedDateTime());
        return this.orderItemInvoicingRepository.save(orderItemInvoicing);
    }

    public OrderItemInvoicing create(OrderItemInvoicing orderItemInvoicing) {
        orderItemInvoicing.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        orderItemInvoicing.setStatus(OrderItemInvoicingStatus.CREATED);
        return this.save(orderItemInvoicing);
    }

    public OrderItemInvoicingDto create(OrderItemInvoicingDto dto) {
        return this.orderItemInvoicingMapper.toDto(this.create(this.orderItemInvoicingMapper.fromDto(dto)));
    }
}
