package fi.hel.verkkokauppa.order.service.invoice;

import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.invoice.InvoiceDto;
import fi.hel.verkkokauppa.order.mapper.InvoiceMapper;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.activemq.broker-url=failover:(tcp://localhost:61616)?startupMaxReconnectAttempts=1")
@RunIfProfile(profile = "local")
class InvoiceServiceTest extends TestUtils {

    @Autowired
    OrderRepository orderRepository;
    @Autowired
    InvoiceService invoiceService;

    @Autowired
    InvoiceMapper invoiceMapper;


    @Test
    public void assertTrue(){
        Assertions.assertTrue(true);
    }

    @Test
    @RunIfProfile(profile = "local")
    void saveInvoiceToOrder() {
        OrderAggregateDto createOrderResponse = createNewOrderToDatabase(1).getBody();
        assert createOrderResponse != null;
        Order order = orderRepository.findById(createOrderResponse.getOrder().getOrderId()).get();

        InvoiceDto invoiceDto = InvoiceDto.builder()
                .orderId(order.getOrderId())
                .userId(order.getUser())
                .businessId("test-businessId")
                .name("test-name")
                .address("test-address")
                .postcode("test-postcode")
                .city("test-city")
                .ovtId("test-ovtId")
                .build();

        Order createdOrderWithInvoice = invoiceService.saveInvoiceToOrder(invoiceDto, order);

        InvoiceDto fromCreatedInvoiceModel = invoiceMapper.toDto(createdOrderWithInvoice.getInvoice());
        Assertions.assertEquals(invoiceDto.getBusinessId(), fromCreatedInvoiceModel.getBusinessId());
        Assertions.assertEquals(invoiceDto.getName(), fromCreatedInvoiceModel.getName());
        Assertions.assertEquals(invoiceDto.getAddress(), fromCreatedInvoiceModel.getAddress());
        Assertions.assertEquals(invoiceDto.getPostcode(), fromCreatedInvoiceModel.getPostcode());
        Assertions.assertEquals(invoiceDto.getCity(), fromCreatedInvoiceModel.getCity());
        Assertions.assertEquals(invoiceDto.getOvtId(), fromCreatedInvoiceModel.getOvtId());

    }
}