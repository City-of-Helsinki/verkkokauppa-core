package fi.hel.verkkokauppa.order.service.invoice;

import fi.hel.verkkokauppa.common.constants.TypePrefix;
import fi.hel.verkkokauppa.common.id.IncrementId;
import fi.hel.verkkokauppa.order.api.data.invoice.InvoiceDto;
import fi.hel.verkkokauppa.order.mapper.InvoiceMapper;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.invoice.Invoice;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InvoiceService {
    final OrderRepository orderRepository;

    final InvoiceMapper invoiceMapper;

    final IncrementId incrementId;

    @Autowired
    public InvoiceService(OrderRepository orderRepository, InvoiceMapper invoiceMapper, IncrementId incrementId) {
        this.orderRepository = orderRepository;
        this.invoiceMapper = invoiceMapper;
        this.incrementId = incrementId;
    }

    public Order saveInvoiceToOrder(InvoiceDto invoiceDto, Order order) {
        Invoice invoice = invoiceMapper.fromDto(invoiceDto);
        invoice.setInvoiceId(order.getInvoice() == null || order.getInvoice().getInvoiceId().isEmpty()
                ? generateInvoiceId()
                : order.getInvoice().getInvoiceId()
        );
        order.setInvoice(invoice);
        return orderRepository.save(order);
    }

    protected String generateInvoiceId(){
        Long invoiceIncrement = incrementId.generateInvoiceIncrementId();
        return TypePrefix.INVOICE.number + String.format("%09d", invoiceIncrement);
    }
}
