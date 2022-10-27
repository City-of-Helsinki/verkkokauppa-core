package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class PaytrailPaymentControllerTest {

    @Autowired
    private PaytrailPaymentController paytrailPaymentController;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PayerRepository payerRepository;

    @Autowired
    private PaymentItemRepository paymentItemRepository;

    private ArrayList<String> toBeDeletedPaymentId = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            toBeDeletedPaymentId.forEach(paymentId -> {
                log.info(paymentId);
                paymentRepository.deleteById(paymentId);
                payerRepository.deleteByPaymentId(paymentId);
                paymentItemRepository.deleteByPaymentId(paymentId);
            });
            // Clear list because all deleted
            toBeDeletedPaymentId = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedPaymentId = new ArrayList<>();
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCreatePaymentFromOrder() {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId("01fde0e9-82b2-4846-acc0-94291625192b");
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        OrderItemDto dummyOrderItem = orderWrapper.getItems().get(0);
        paymentRequestDataDto.setOrder(orderWrapper);

        ResponseEntity<Payment> paymentResponse = paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        Payment payment = paymentResponse.getBody();

        if (payment.getPaymentId() != null) {
            /* Check payment */
            assertEquals(orderWrapper.getOrder().getOrderId(), payment.getOrderId());

            /* Check payment items */
            String paymentId = payment.getPaymentId();
            List<PaymentItem> paymentItems = paymentItemRepository.findByPaymentId(payment.getPaymentId());
            assertEquals(1, paymentItems.size());

            PaymentItem paymentItem = paymentItems.get(0);
            assertEquals(orderWrapper.getOrder().getOrderId(), paymentItem.getOrderId());
            assertEquals(dummyOrderItem.getProductId(), paymentItem.getProductId());

            /* Check payer */
            List<Payer> payers = payerRepository.findByPaymentId(payment.getPaymentId());
            assertEquals(1, payers.size());

            Payer payer = payers.get(0);
            assertEquals(orderWrapper.getOrder().getCustomerFirstName(), payer.getFirstName());
            assertEquals(orderWrapper.getOrder().getCustomerLastName(), payer.getLastName());
            assertEquals(orderWrapper.getOrder().getCustomerEmail(), payer.getEmail());

            toBeDeletedPaymentId.add(paymentId);
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCreatePaymentFromOrderWithoutUser() {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId("01fde0e9-82b2-4846-acc0-94291625192b");
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        orderWrapper.getOrder().setUser("");
        paymentRequestDataDto.setOrder(orderWrapper);

        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        });
        Assertions.assertEquals(CommonApiException.class, exception.getClass());
        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        Assertions.assertEquals("rejected-creating-payment-for-order-without-user", exception.getErrors().getErrors().get(0).getCode());
        Assertions.assertEquals("rejected creating payment for order without user, order id [" + orderWrapper.getOrder().getOrderId() + "]", exception.getErrors().getErrors().get(0).getMessage());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCreatePaymentFromOrderWithInvalidStatus() {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId("01fde0e9-82b2-4846-acc0-94291625192b");

        /* Test with OrderStatus.DRAFT */
        OrderWrapper orderWrapper1 = createDummyOrderWrapper();
        orderWrapper1.getOrder().setStatus("draft");
        paymentRequestDataDto.setOrder(orderWrapper1);

        CommonApiException exception1 = assertThrows(CommonApiException.class, () -> {
            paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        });
        Assertions.assertEquals(CommonApiException.class, exception1.getClass());
        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception1.getStatus());
        Assertions.assertEquals("rejected-creating-payment-for-unconfirmed-order", exception1.getErrors().getErrors().get(0).getCode());
        Assertions.assertEquals("rejected creating payment for unconfirmed order, order id [" + orderWrapper1.getOrder().getOrderId() + "]", exception1.getErrors().getErrors().get(0).getMessage());

        /* Test with OrderStatus.CANCELLED */
        OrderWrapper orderWrapper2 = createDummyOrderWrapper();
        orderWrapper2.getOrder().setStatus("cancelled");
        paymentRequestDataDto.setOrder(orderWrapper2);

        CommonApiException exception2 = assertThrows(CommonApiException.class, () -> {
            paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        });
        Assertions.assertEquals(CommonApiException.class, exception2.getClass());
        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception2.getStatus());
        Assertions.assertEquals("rejected-creating-payment-for-unconfirmed-order", exception2.getErrors().getErrors().get(0).getCode());
        Assertions.assertEquals("rejected creating payment for unconfirmed order, order id [" + orderWrapper2.getOrder().getOrderId() + "]", exception2.getErrors().getErrors().get(0).getMessage());
    }

    private OrderWrapper createDummyOrderWrapper() {
        OrderDto orderDto = new OrderDto();
        String orderId = UUID.randomUUID().toString();
        orderDto.setOrderId(orderId);
        orderDto.setNamespace("venepaikat");
        orderDto.setUser("dummy_user");
        orderDto.setCreatedAt("");
        orderDto.setStatus("confirmed");
        orderDto.setType("order");
        orderDto.setCustomerFirstName("Martin");
        orderDto.setCustomerLastName("Leh");
        orderDto.setCustomerEmail("testi@ambientia.fi");
        orderDto.setPriceNet("1234");
        orderDto.setPriceVat("0");
        // Sets total price to be 1 eur
        orderDto.setPriceTotal("1234");

        OrderWrapper order = new OrderWrapper();
        order.setOrder(orderDto);

        List<OrderItemDto> items = new ArrayList<>();

        OrderItemDto orderItem = new OrderItemDto();

        String orderItemId = UUID.randomUUID().toString();
        orderItem.setOrderItemId(orderItemId);
        orderItem.setPriceGross(BigDecimal.valueOf(1234));
        orderItem.setQuantity(1);
        orderItem.setVatPercentage("24");
        orderItem.setProductId("test-product-id");
        orderItem.setProductName("productName");
        orderItem.setOrderId(orderId);

        items.add(orderItem);

        order.setItems(items);
        return order;
    }
}
