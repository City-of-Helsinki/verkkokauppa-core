package fi.hel.verkkokauppa.order.service.invoice;

import fi.hel.verkkokauppa.order.api.data.invoice.InvoiceDto;
import fi.hel.verkkokauppa.order.mapper.InvoiceMapper;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InvoiceService {
    final OrderRepository orderRepository;

    final InvoiceMapper invoiceMapper;

    @Autowired
    public InvoiceService(OrderRepository orderRepository, InvoiceMapper invoiceMapper) {
        this.orderRepository = orderRepository;
        this.invoiceMapper = invoiceMapper;
    }

    public Order saveInvoiceToOrder(InvoiceDto invoice, Order order) {
        order.setInvoice(invoiceMapper.fromDto(invoice));
        return orderRepository.save(order);
    }
}
