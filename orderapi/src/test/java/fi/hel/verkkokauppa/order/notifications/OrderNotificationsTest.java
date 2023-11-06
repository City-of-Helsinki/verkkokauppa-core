package fi.hel.verkkokauppa.order.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformer;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderPaymentMethod;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderPaymentMethodRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunIfProfile(profile = "local")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@Slf4j
public class OrderNotificationsTest extends DummyData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderPaymentMethodRepository orderPaymentMethodRepository;

    @Autowired
    private OrderItemTransformer orderItemTransformer;
    @Autowired
    private OrderTransformer orderTransformer;

    @Autowired
    private SendNotificationService sendNotificationService;



    @After
    public void tearDown() {
        try {

        } catch (Exception e) {
            log.info("delete error {}", e.toString());

        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testSendOrderMessageNotification() throws Exception {
//        Order order = generateDummyOrder();
//        order.setOrderId(UUID.randomUUID().toString());
//        order.setNamespace("venepaikat");
//        order = orderRepository.save(order);

        // First test creation
//        String testOrderId = order.getOrderId();
//        String testUserId = order.getUser();
        OrderMessage.OrderMessageBuilder orderMessageBuilder = OrderMessage.builder()
                .eventType(EventType.SUBSCRIPTION_RENEWAL_ORDER_CREATED)
                .eventTimestamp(DateTimeUtil.getDateTime())
                .namespace("venepaikat")
                .orderId(UUID.randomUUID().toString())
                .timestamp(DateTimeUtil.getDateTime())
                .orderType("subscription")
                .priceTotal("10")
                .priceNet("8")
                .priceVat("2");

        sendNotificationService.sendOrderMessageNotification(orderMessageBuilder.build());

    }

}
