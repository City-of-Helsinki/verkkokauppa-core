package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.logic.builder.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.logic.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.impl.PaytrailCreatePaymentRequestConverter;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailPaymentClient;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import fi.hel.verkkokauppa.payment.testing.utils.AutoMockBeanFactory;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@WebMvcTest(PaytrailPaymentController.class)
@Import(value = {
        PaytrailPaymentController.class,
        PaytrailPaymentClient.class,
        PaytrailAuthClientFactory.class,
        PaytrailCreatePaymentRequestConverter.class
})
@ContextConfiguration(classes = {AutoMockBeanFactory.class, ValidationAutoConfiguration.class}) // This automatically mocks missing beans
@ActiveProfiles("test")
@Slf4j
public class PaytrailPaymentControllerUnitTests {

    @Value("${paytrail.aggregate.merchant.id}")
    private String aggregateMerchantId;

    @Value("${paytrail.test.shop.id}")
    private String shopInShopMerchantId;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PaytrailPaymentClient paytrailPaymentClient;

    @MockBean
    private PaymentPaytrailService paymentPaytrailService;

    @MockBean
    private PaytrailPaymentContextBuilder paymentContextBuilder;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private PayerRepository payerRepository;

    @MockBean
    private PaymentItemRepository paymentItemRepository;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(paymentPaytrailService, "paymentContextBuilder", paymentContextBuilder);
        ReflectionTestUtils.setField(paymentPaytrailService, "paytrailPaymentClient", paytrailPaymentClient);
        ReflectionTestUtils.setField(paymentPaytrailService, "paymentContextBuilder", paymentContextBuilder);
        ReflectionTestUtils.setField(paymentPaytrailService, "paymentRepository", paymentRepository);
        ReflectionTestUtils.setField(paymentPaytrailService, "paymentItemRepository", paymentItemRepository);
        ReflectionTestUtils.setField(paymentPaytrailService, "payerRepository", payerRepository);
    }

    @Test
    public void whenCreateFromOrderWithValidDataThenReturnStatus201() throws Exception {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId("01fde0e9-82b2-4846-acc0-94291625192b");
        paymentRequestDataDto.setLanguage("FI");
        paymentRequestDataDto.setPaymentMethod("nordea");
        paymentRequestDataDto.setPaymentMethodLabel("Nordea");

        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String mockPaymentId = PaymentUtil.generatePaymentOrderNumber(orderWrapper.getOrder().getOrderId());
        Payment mockPayment = mockCreateValidPaymentFromOrder(paymentRequestDataDto, mockPaymentId);

        MvcResult response = this.mockMvc.perform(
                        post("/payment/paytrail/createFromOrder")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(paymentRequestDataDto))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(201))
                .andReturn();
        Payment responseDto = mapper.readValue(response.getResponse().getContentAsString(), Payment.class);
        Assertions.assertNotNull(responseDto);

        /*
        * Not able to verify exact paymentId for mock and actual payment.
        * PaymentId includes orderId and timestamp, so the timestamp of mock paymentId will differ from actual payment.
        * Here instead we extract orderId from paymentId and verify it matches.
        */
        String expectedOrderIdFromPaymentId = mockPayment.getPaymentId().split("_", 2)[0];
        String responseOrderIdFromPaymentId = responseDto.getPaymentId().split("_", 2)[0];

        Assertions.assertEquals(expectedOrderIdFromPaymentId, responseOrderIdFromPaymentId);
        Assertions.assertEquals(responseDto.getOrderId(), mockPayment.getOrderId());
        Assertions.assertEquals(responseDto.getNamespace(), mockPayment.getNamespace());
        Assertions.assertEquals(responseDto.getUserId(), mockPayment.getUserId());
        Assertions.assertEquals(responseDto.getPaymentMethod(), mockPayment.getPaymentMethod());
    }

    @Test
    public void whenCreateFromOrderWithInvalidPaymentContextThenReturnStatus500() {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId("01fde0e9-82b2-4846-acc0-94291625192b");
        paymentRequestDataDto.setLanguage("FI");
        paymentRequestDataDto.setPaymentMethod("nordea");
        paymentRequestDataDto.setPaymentMethodLabel("Nordea");

        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String mockPaymentId = PaymentUtil.generatePaymentOrderNumber(orderWrapper.getOrder().getOrderId());
        mockCreateInvalidPaymentFromOrder(paymentRequestDataDto, mockPaymentId);

        Exception exception = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                            post("/payment/paytrail/createFromOrder")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(paymentRequestDataDto))
                    )
                    .andDo(print())
                    .andExpect(status().is5xxServerError())
                    .andExpect(status().is(500));
        });

        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals("failed-to-create-paytrail-payment", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("Failed to create paytrail payment", cause.getErrors().getErrors().get(0).getMessage());
    }

    private Payment mockCreateValidPaymentFromOrder(GetPaymentRequestDataDto paymentRequestDataDto, String mockPaymentId) {
        OrderWrapper orderWrapper = paymentRequestDataDto.getOrder();
        PaytrailPaymentContext mockPaymentContext = createMockPaytrailPaymentContext(orderWrapper.getOrder().getNamespace());
        Payment mockPayment = createMockPayment(paymentRequestDataDto, orderWrapper.getOrder().getType(), mockPaymentId);
        PaymentItem mockPaymentItem = createMockPaymentItem(mockPaymentId, orderWrapper.getItems().get(0));
        Payer mockPayer = createMockPayer(mockPaymentId, orderWrapper.getOrder());

        mockCreatePaymentFlow(mockPaymentContext, mockPayment, mockPaymentItem, mockPayer);

        return mockPayment;
    }

    private Payment mockCreateInvalidPaymentFromOrder(GetPaymentRequestDataDto paymentRequestDataDto, String mockPaymentId) {
        OrderWrapper orderWrapper = paymentRequestDataDto.getOrder();
        PaytrailPaymentContext mockPaymentContext = createMockPaytrailPaymentContext(orderWrapper.getOrder().getNamespace());
        mockPaymentContext.setAggregateMerchantId("invalid-123");
        mockPaymentContext.setShopId("invalid-456");
        Payment mockPayment = createMockPayment(paymentRequestDataDto, orderWrapper.getOrder().getType(), mockPaymentId);
        PaymentItem mockPaymentItem = createMockPaymentItem(mockPaymentId, orderWrapper.getItems().get(0));
        Payer mockPayer = createMockPayer(mockPaymentId, orderWrapper.getOrder());

        mockCreatePaymentFlow(mockPaymentContext, mockPayment, mockPaymentItem, mockPayer);

        return mockPayment;
    }

    private void mockCreatePaymentFlow(PaytrailPaymentContext mockPaymentContext, Payment mockPayment, PaymentItem mockPaymentItem, Payer mockPayer) {
        Mockito.when(paymentPaytrailService.getPaymentRequestData(Mockito.any(GetPaymentRequestDataDto.class))).thenCallRealMethod();
        Mockito.when(paymentContextBuilder.buildFor(Mockito.any(String.class), Mockito.any(String.class))).thenReturn(mockPaymentContext);
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenReturn(mockPayment);
        Mockito.when(paymentItemRepository.save(Mockito.any(PaymentItem.class))).thenReturn(mockPaymentItem);
        Mockito.when(payerRepository.save(Mockito.any(Payer.class))).thenReturn(mockPayer);
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

    private PaytrailPaymentContext createMockPaytrailPaymentContext(String namespace) {
        PaytrailPaymentContext mockPaymentContext = new PaytrailPaymentContext();
        mockPaymentContext.setNamespace(namespace);
        mockPaymentContext.setAggregateMerchantId(aggregateMerchantId);
        mockPaymentContext.setShopId(shopInShopMerchantId);
        mockPaymentContext.setDefaultCurrency("EUR");
        mockPaymentContext.setDefaultLanguage("FI");
        mockPaymentContext.setReturnUrl("https://ecom.example.com/cart");
        mockPaymentContext.setNotifyUrl("https://ecom.example.com/cart");
        mockPaymentContext.setCp("PRO-31312-1");

        return mockPaymentContext;
    }

    private Payment createMockPayment(GetPaymentRequestDataDto dto, String type, String paymentId) {
        OrderDto order = dto.getOrder().getOrder();

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setNamespace(order.getNamespace());
        payment.setOrderId(order.getOrderId());
        payment.setUserId(order.getUser());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setPaymentMethodLabel(dto.getPaymentMethodLabel());
        payment.setTimestamp(sdf.format(timestamp));
        payment.setAdditionalInfo("{\"payment_method\": " + dto.getPaymentMethod() + "}");
        payment.setPaymentType(type);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setTotalExclTax(new BigDecimal(order.getPriceNet()));
        payment.setTaxAmount(new BigDecimal(order.getPriceVat()));
        payment.setTotal(new BigDecimal(order.getPriceTotal()));
        payment.setPaytrailTransactionId("2e60d1e0-0f5a-4f1b-ad95-d9b0fc00202a");

        return payment;
    }

    private PaymentItem createMockPaymentItem(String paymentId, OrderItemDto itemDto) {
        PaymentItem item = new PaymentItem();
        item.setPaymentId(paymentId);
        item.setOrderId(itemDto.getOrderId());
        item.setProductId(itemDto.getProductId());
        item.setProductName(itemDto.getProductName());
        item.setQuantity(item.getQuantity());
        item.setRowPriceNet(itemDto.getRowPriceNet());
        item.setRowPriceVat(itemDto.getRowPriceVat());
        item.setRowPriceTotal(itemDto.getRowPriceTotal());
        item.setTaxPercent(itemDto.getVatPercentage());
        item.setPriceNet(itemDto.getPriceNet());
        item.setTaxAmount(itemDto.getPriceVat());
        item.setPriceGross(itemDto.getPriceGross());
        return item;
    }

    private Payer createMockPayer(String paymentId, OrderDto orderDto) {
        Payer payer = new Payer();
        payer.setPaymentId(paymentId);
        payer.setFirstName(orderDto.getCustomerFirstName());
        payer.setLastName(orderDto.getCustomerLastName());
        payer.setEmail(orderDto.getCustomerEmail());
        return payer;
    }


}
