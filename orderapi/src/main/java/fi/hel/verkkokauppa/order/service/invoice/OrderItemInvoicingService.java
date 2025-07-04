package fi.hel.verkkokauppa.order.service.invoice;

import fi.hel.verkkokauppa.common.constants.TypePrefix;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.invoice.OrderItemInvoicingDto;
import fi.hel.verkkokauppa.order.mapper.OrderItemInvoicingMapper;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderStatus;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicing;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemInvoicingRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class OrderItemInvoicingService {
    final OrderItemInvoicingRepository orderItemInvoicingRepository;

    final OrderItemInvoicingMapper orderItemInvoicingMapper;

    final OrderService orderService;

    @Autowired
    public OrderItemInvoicingService(OrderItemInvoicingRepository orderItemInvoicingRepository, OrderItemInvoicingMapper orderItemInvoicingMapper, OrderService orderService) {
        this.orderItemInvoicingRepository = orderItemInvoicingRepository;
        this.orderItemInvoicingMapper = orderItemInvoicingMapper;
        this.orderService = orderService;
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

    public List<OrderItemInvoicing> findInvoicingsToExport() {
        return orderItemInvoicingRepository.findAllByInvoicingDateLessThanEqualAndStatus(LocalDate.now(), OrderItemInvoicingStatus.CREATED);
    }

    public void markInvoicingsInvoiced(List<OrderItemInvoicing> orderItemInvoicings) {
        for (OrderItemInvoicing orderItemInvoicing : orderItemInvoicings) {
            orderItemInvoicing.setStatus(OrderItemInvoicingStatus.INVOICED);
            save(orderItemInvoicing);
        }
    }

    public List<OrderItemInvoicing> filterAndUpdateCancelledOrders(List<OrderItemInvoicing> orderItemInvoicings) {
        List<OrderItemInvoicing> filteredInvoicings = new ArrayList<>();
        for (OrderItemInvoicing orderItemInvoicing : orderItemInvoicings) {
            Order order = orderService.findById(orderItemInvoicing.getOrderId());
            if (order == null) {
                throw new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("failed-to-get-order", "failed to get order with id [" + orderItemInvoicing.getOrderId() + "]")
                );
            }
            if (order.getStatus().equals(OrderStatus.CANCELLED)) {
                orderItemInvoicing.setStatus(OrderItemInvoicingStatus.CANCELLED);
                save(orderItemInvoicing);
            } else {
                filteredInvoicings.add(orderItemInvoicing);
            }
        }
        return filteredInvoicings;
    }

    public Boolean canCancel(String orderId){
        List<OrderItemInvoicing> orderItemInvoicings = orderItemInvoicingRepository.findByOrderId(orderId);

        if( orderItemInvoicings == null || orderItemInvoicings.isEmpty() ){
            // no order item invoicings, no reason why could not be cancelled
            return true;
        }

        for (OrderItemInvoicing orderItemInvoicing : orderItemInvoicings) {
            if( orderItemInvoicing.getStatus().equals(OrderItemInvoicingStatus.CREATED) ){
                // there is something that can be cancelled
                return true;
            }
        }
        log.debug("Order has nothing to cancel. OrderId: {}", orderId);
        return false;
    }

    public void cancelOrderItemInvoicings(String orderId){
        List<OrderItemInvoicing> orderItemInvoicings = orderItemInvoicingRepository.findByOrderId(orderId);

        if( orderItemInvoicings == null || orderItemInvoicings.isEmpty() ){
            // no order item invoicings to cancel
            return;
        }

        log.debug("Cancelling order item invoicings for order: {}", orderId);
        for (OrderItemInvoicing orderItemInvoicing : orderItemInvoicings) {
            if( orderItemInvoicing.getStatus().equals(OrderItemInvoicingStatus.CREATED) ){
                // there is something that can be cancelled
                orderItemInvoicing.setStatus(OrderItemInvoicingStatus.CANCELLED);
                save(orderItemInvoicing);
            }
        }
    }


}
