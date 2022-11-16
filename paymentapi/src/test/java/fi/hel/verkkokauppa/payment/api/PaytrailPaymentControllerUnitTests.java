package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.validation.PaytrailPaymentReturnValidator;
import fi.hel.verkkokauppa.payment.paytrail.converter.impl.PaytrailCreatePaymentPayloadConverter;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailPaymentClient;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import fi.hel.verkkokauppa.payment.testing.utils.AutoMockBeanFactory;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import fi.hel.verkkokauppa.payment.utils.TestPaymentCreator;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;


@WebMvcTest(PaytrailPaymentController.class)
@Import(value = {
        PaytrailPaymentController.class,
        PaytrailPaymentClient.class,
        PaytrailAuthClientFactory.class,
        PaytrailCreatePaymentPayloadConverter.class
})
@ContextConfiguration(classes = {AutoMockBeanFactory.class, ValidationAutoConfiguration.class}) // This automatically mocks missing beans
@TestPropertySource(properties = {
    "payment_default_paytrail_return_success_url=https://webhook.site/ef83f10a-721b-44ab-bac8-241e381a98e5",
    "payment_default_paytrail_return_cancel_url=https://webhook.site/ef83f10a-721b-44ab-bac8-241e381a98e5",
    "payment_default_paytrail_notify_success_url=https://webhook.site/ef83f10a-721b-44ab-bac8-241e381a98e5",
    "payment_default_paytrail_notify_cancel_url=https://webhook.site/ef83f10a-721b-44ab-bac8-241e381a98e5"
})
@Slf4j
public class PaytrailPaymentControllerUnitTests {
    
    private static final String TEST_MERCHANT_ID = "01fde0e9-82b2-4846-acc0-94291625192b";
    private static final String PAYTRAIL_MERCHANT_ID = "375917";
    private static final String PAYTRAIL_SECRET_KEY = "SAIPPUAKAUPPIAS";
    private static final String PAYTRAIL_AGGREGATE_SECRET_KEY = "MONISAIPPUAKAUPPIAS";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PaytrailPaymentClient paytrailPaymentClient;

    @MockBean
    private PaymentPaytrailService paymentPaytrailService;

    @MockBean
    private OnlinePaymentService onlinePaymentService;

    @MockBean
    private PaytrailPaymentReturnValidator paytrailPaymentReturnValidator;

    @MockBean
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

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
        ReflectionTestUtils.setField(paytrailPaymentReturnValidator, "aggregateSecretKey", PAYTRAIL_AGGREGATE_SECRET_KEY);
        ReflectionTestUtils.setField(paytrailPaymentReturnValidator, "commonServiceConfigurationClient", commonServiceConfigurationClient);
        ReflectionTestUtils.setField(paytrailPaymentReturnValidator, "paymentRepository", paymentRepository);
    }

    @Test
    public void whenCreateFromOrderWithValidDataThenReturnStatus201() throws Exception {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId(TEST_MERCHANT_ID);
        paymentRequestDataDto.setLanguage("FI");
        paymentRequestDataDto.setPaymentMethod("nordea");
        paymentRequestDataDto.setPaymentMethodLabel("Nordea");

        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String mockPaymentId = PaymentUtil.generatePaymentOrderNumber(orderWrapper.getOrder().getOrderId());
        PaytrailPaymentContext mockPaymentContext = createMockPaytrailPaymentContext(orderWrapper.getOrder().getNamespace(), paymentRequestDataDto.getMerchantId());
        Payment mockPayment = mockCreateValidPaymentFromOrder(paymentRequestDataDto, mockPaymentContext, mockPaymentId);

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
        assertNotNull(responseDto);

        /*
        * Not able to verify exact paymentId for mock and actual payment.
        * PaymentId includes orderId and timestamp, so the timestamp of mock paymentId will differ from actual payment.
        * Here instead we extract orderId from paymentId and verify it matches.
        */
        String expectedOrderIdFromPaymentId = mockPayment.getPaymentId().split("_", 2)[0];
        String responseOrderIdFromPaymentId = responseDto.getPaymentId().split("_", 2)[0];

        assertEquals(expectedOrderIdFromPaymentId, responseOrderIdFromPaymentId);
        assertEquals(responseDto.getOrderId(), mockPayment.getOrderId());
        assertEquals(responseDto.getNamespace(), mockPayment.getNamespace());
        assertEquals(responseDto.getUserId(), mockPayment.getUserId());
        assertEquals(responseDto.getPaymentMethod(), mockPayment.getPaymentMethod());
    }

    @Test
    public void whenCreateFromOrderWithInvalidPaymentContextThenReturnStatus500() {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId(TEST_MERCHANT_ID);
        paymentRequestDataDto.setLanguage("FI");
        paymentRequestDataDto.setPaymentMethod("nordea");
        paymentRequestDataDto.setPaymentMethodLabel("Nordea");

        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String mockPaymentId = PaymentUtil.generatePaymentOrderNumber(orderWrapper.getOrder().getOrderId());
        PaytrailPaymentContext mockPaymentContext = createMockPaytrailPaymentContext(orderWrapper.getOrder().getNamespace(), paymentRequestDataDto.getMerchantId());
        mockPaymentContext.setPaytrailMerchantId("invalid-123");
        mockCreateValidPaymentFromOrder(paymentRequestDataDto, mockPaymentContext, mockPaymentId);

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
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, cause.getStatus());
        assertEquals("failed-to-create-paytrail-payment", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("Failed to create paytrail payment", cause.getErrors().getErrors().get(0).getMessage());
    }

    @Test
    public void whenCreateFromOrderWithoutUserStatusThenReturnStatus403() throws Exception {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId(TEST_MERCHANT_ID);
        paymentRequestDataDto.setLanguage("FI");
        paymentRequestDataDto.setPaymentMethod("nordea");
        paymentRequestDataDto.setPaymentMethodLabel("Nordea");

        OrderWrapper orderWrapper = createDummyOrderWrapper();
        orderWrapper.getOrder().setUser("");
        paymentRequestDataDto.setOrder(orderWrapper);

        String mockPaymentId = PaymentUtil.generatePaymentOrderNumber(orderWrapper.getOrder().getOrderId());
        PaytrailPaymentContext mockPaymentContext = createMockPaytrailPaymentContext(orderWrapper.getOrder().getNamespace(), paymentRequestDataDto.getMerchantId());
        mockCreateValidPaymentFromOrder(paymentRequestDataDto, mockPaymentContext, mockPaymentId);

        Exception exception = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                post("/payment/paytrail/createFromOrder")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(paymentRequestDataDto))
            )
            .andDo(print());
        });
        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals(HttpStatus.FORBIDDEN, cause.getStatus());
        assertEquals("rejected-creating-paytrail-payment-for-order-without-user", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("rejected creating paytrail payment for order without user, order id [" + orderWrapper.getOrder().getOrderId() + "]", cause.getErrors().getErrors().get(0).getMessage());

    }

    @Test
    public void whenCreateFromOrderWithInvalidOrderStatusThenReturnStatus403() throws Exception {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId(TEST_MERCHANT_ID);
        paymentRequestDataDto.setLanguage("FI");
        paymentRequestDataDto.setPaymentMethod("nordea");
        paymentRequestDataDto.setPaymentMethodLabel("Nordea");

        /* Test with OrderStatus.DRAFT */
        OrderWrapper orderWrapper1 = createDummyOrderWrapper();
        orderWrapper1.getOrder().setStatus("draft");
        paymentRequestDataDto.setOrder(orderWrapper1);

        String mockPaymentId1 = PaymentUtil.generatePaymentOrderNumber(orderWrapper1.getOrder().getOrderId());
        PaytrailPaymentContext mockPaymentContext1 = createMockPaytrailPaymentContext(orderWrapper1.getOrder().getNamespace(), paymentRequestDataDto.getMerchantId());
        mockCreateValidPaymentFromOrder(paymentRequestDataDto, mockPaymentContext1, mockPaymentId1);

        Exception exception1 = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                post("/payment/paytrail/createFromOrder")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(paymentRequestDataDto))
            )
            .andDo(print());
        });
        CommonApiException cause1 = (CommonApiException) exception1.getCause();
        assertEquals(CommonApiException.class, cause1.getClass());
        assertEquals(HttpStatus.FORBIDDEN, cause1.getStatus());
        assertEquals("rejected-creating-paytrail-payment-for-unconfirmed-order", cause1.getErrors().getErrors().get(0).getCode());
        assertEquals("rejected creating paytrail payment for unconfirmed order, order id [" + orderWrapper1.getOrder().getOrderId() + "]", cause1.getErrors().getErrors().get(0).getMessage());

        /* Test with OrderStatus.CANCELLED */
        OrderWrapper orderWrapper2 = createDummyOrderWrapper();
        orderWrapper2.getOrder().setStatus("cancelled");
        paymentRequestDataDto.setOrder(orderWrapper2);

        String mockPaymentId2 = PaymentUtil.generatePaymentOrderNumber(orderWrapper2.getOrder().getOrderId());
        PaytrailPaymentContext mockPaymentContext2 = createMockPaytrailPaymentContext(orderWrapper2.getOrder().getNamespace(), paymentRequestDataDto.getMerchantId());
        mockCreateValidPaymentFromOrder(paymentRequestDataDto, mockPaymentContext2, mockPaymentId2);

        Exception exception2 = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                            post("/payment/paytrail/createFromOrder")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(paymentRequestDataDto))
                    )
                    .andDo(print());
        });
        CommonApiException cause2 = (CommonApiException) exception2.getCause();
        assertEquals(CommonApiException.class, cause2.getClass());
        assertEquals(HttpStatus.FORBIDDEN, cause2.getStatus());
        assertEquals("rejected-creating-paytrail-payment-for-unconfirmed-order", cause2.getErrors().getErrors().get(0).getCode());
        assertEquals("rejected creating paytrail payment for unconfirmed order, order id [" + orderWrapper2.getOrder().getOrderId() + "]", cause2.getErrors().getErrors().get(0).getMessage());
    }

    @Test
    public void whenCreateFromOrderWithInvalidShopInShopPaymentContextThenReturnStatus403() throws Exception {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId(TEST_MERCHANT_ID);
        paymentRequestDataDto.setLanguage("FI");
        paymentRequestDataDto.setPaymentMethod("nordea");
        paymentRequestDataDto.setPaymentMethodLabel("Nordea");

        /* Test with missing shopId context */
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String mockPaymentId = PaymentUtil.generatePaymentOrderNumber(orderWrapper.getOrder().getOrderId());
        PaytrailPaymentContext mockPaymentContext = createMockPaytrailPaymentContext(orderWrapper.getOrder().getNamespace(), paymentRequestDataDto.getMerchantId());
        mockPaymentContext.setUseShopInShop(true);
        mockPaymentContext.setShopId(null);
        mockCreateValidPaymentFromOrder(paymentRequestDataDto, mockPaymentContext, mockPaymentId);

        Exception exception = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                            post("/payment/paytrail/createFromOrder")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(paymentRequestDataDto))
                    )
                    .andDo(print());
        });
        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals(HttpStatus.FORBIDDEN, cause.getStatus());
        assertEquals("validation-failed-for-paytrail-payment-context-without-merchant-shop-id", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("Failed to validate paytrail payment context, merchant shop id not found for merchant [" + paymentRequestDataDto.getMerchantId() + "]", cause.getErrors().getErrors().get(0).getMessage());
    }

    @Test
    public void whenCreateFromOrderWithInvalidNormalPaymentContextThenReturnStatus403() throws Exception {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId(TEST_MERCHANT_ID);
        paymentRequestDataDto.setLanguage("FI");
        paymentRequestDataDto.setPaymentMethod("nordea");
        paymentRequestDataDto.setPaymentMethodLabel("Nordea");

        /* Test with missing shopId context */
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String mockPaymentId = PaymentUtil.generatePaymentOrderNumber(orderWrapper.getOrder().getOrderId());
        PaytrailPaymentContext mockPaymentContext = createMockPaytrailPaymentContext(orderWrapper.getOrder().getNamespace(), paymentRequestDataDto.getMerchantId());
        mockPaymentContext.setUseShopInShop(false);
        mockPaymentContext.setPaytrailMerchantId(null);
        mockPaymentContext.setPaytrailSecretKey(null);
        mockCreateValidPaymentFromOrder(paymentRequestDataDto, mockPaymentContext, mockPaymentId);

        Exception exception = assertThrows(Exception.class, () -> {
            this.mockMvc.perform(
                            post("/payment/paytrail/createFromOrder")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(paymentRequestDataDto))
                    )
                    .andDo(print());
        });
        CommonApiException cause = (CommonApiException) exception.getCause();
        assertEquals(CommonApiException.class, cause.getClass());
        assertEquals(HttpStatus.FORBIDDEN, cause.getStatus());
        assertEquals("validation-failed-for-paytrail-payment-context-without-paytrail-merchant-credentials", cause.getErrors().getErrors().get(0).getCode());
        assertEquals("Failed to validate paytrail payment context, merchant credentials (merchant ID or secret key) are missing for merchant [" + paymentRequestDataDto.getMerchantId() + "]", cause.getErrors().getErrors().get(0).getMessage());
    }

    @Test
    public void whenCheckReturnUrlWithSuccessfulPaymentThenReturnStatus200() throws Exception {
        /* First create mock payment from mock order to make the whole return url check process possible */
        Payment mockPaymentDto = mockCheckReturnUrlProcess();

        /* Create callback and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(mockPaymentDto.getPaymentId(), mockPaymentDto.getPaytrailTransactionId(), mockStatus);

        String mockSettlementReference = "8739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams.put("checkout-settlement-reference", mockSettlementReference);

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, PAYTRAIL_SECRET_KEY);
            mockCallbackCheckoutParams.put("signature", mockSignature);

            /* Convert mock callback checkout params to actual query params */
            String queryParams = mockCallbackCheckoutParams.keySet().stream()
                    .map(key -> key + "=" + mockCallbackCheckoutParams.get(key))
                    .collect(Collectors.joining("&"));
            queryParams += "&" + "merchantId=" + TEST_MERCHANT_ID;
            MvcResult checkReturnUrlResponse = this.mockMvc.perform(
                            get("/payment/paytrail/check-return-url?" + queryParams)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(status().is(200))
                    .andReturn();
            PaymentReturnDto mockPaymentReturnDto = mapper.readValue(checkReturnUrlResponse.getResponse().getContentAsString(), PaymentReturnDto.class);

            /* Verify correct payment return dto */
            assertTrue(mockPaymentReturnDto.isValid());
            assertTrue(mockPaymentReturnDto.isPaymentPaid());
            assertFalse(mockPaymentReturnDto.isAuthorized());
            assertFalse(mockPaymentReturnDto.isCanRetry());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void whenCheckReturnUrlWithInvalidSignatureThenReturnStatus200() throws Exception {
        /* First create mock payment from mock order to make the whole return url check process possible */
        Payment mockPaymentDto = mockCheckReturnUrlProcess();

        /* CASE 1: Create callback with invalid signature on query param and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams1 = createMockCallbackParams(mockPaymentDto.getPaymentId(), mockPaymentDto.getPaytrailTransactionId(), mockStatus);
        String mockSettlementReference1 = "8739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams1.put("checkout-settlement-reference", mockSettlementReference1);

        String mockSignature1 = "invalid-739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams1.put("signature", mockSignature1);

        /* Convert mock callback checkout params to actual query params */
        String queryParams1 = mockCallbackCheckoutParams1.keySet().stream()
                .map(key -> key + "=" + mockCallbackCheckoutParams1.get(key))
                .collect(Collectors.joining("&"));
        queryParams1 += "&" + "merchantId=" + TEST_MERCHANT_ID;

        MvcResult checkReturnUrlResponse1 = this.mockMvc.perform(
                        get("/payment/paytrail/check-return-url?" + queryParams1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200))
                .andReturn();
        PaymentReturnDto mockPaymentReturnDto1 = mapper.readValue(checkReturnUrlResponse1.getResponse().getContentAsString(), PaymentReturnDto.class);

        /* Verify correct payment return dto */
        assertFalse(mockPaymentReturnDto1.isValid());
        assertTrue(mockPaymentReturnDto1.isPaymentPaid());
        assertFalse(mockPaymentReturnDto1.isAuthorized());
        assertFalse(mockPaymentReturnDto1.isCanRetry());


        /* CASE 2: Create callback with valid generated signature and invalidate it by changing one query param then check url */
        Map<String, String> mockCallbackCheckoutParams2 = createMockCallbackParams(mockPaymentDto.getPaymentId(), mockPaymentDto.getPaytrailTransactionId(), mockStatus);
        mockCallbackCheckoutParams2.put("checkout-settlement-reference", mockSettlementReference1);

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams2);
        try {
            String mockSignature2 = PaytrailSignatureService.calculateSignature(filteredParams, null, PAYTRAIL_SECRET_KEY);
            mockCallbackCheckoutParams2.put("signature", mockSignature2);

            /* Override some query param to invalidate the generated signature */
            mockCallbackCheckoutParams2.put("checkout-settlement-reference", UUID.randomUUID().toString());

            /* Convert mock callback checkout params to actual query params */
            String queryParams2 = mockCallbackCheckoutParams2.keySet().stream()
                    .map(key -> key + "=" + mockCallbackCheckoutParams2.get(key))
                    .collect(Collectors.joining("&"));
            queryParams2 += "&" + "merchantId=" + TEST_MERCHANT_ID;
            MvcResult checkReturnUrlResponse2 = this.mockMvc.perform(
                            get("/payment/paytrail/check-return-url?" + queryParams2)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(status().is(200))
                    .andReturn();
            PaymentReturnDto mockPaymentReturnDto2 = mapper.readValue(checkReturnUrlResponse2.getResponse().getContentAsString(), PaymentReturnDto.class);

            /* Verify correct payment return dto */
            assertFalse(mockPaymentReturnDto2.isValid());
            assertTrue(mockPaymentReturnDto2.isPaymentPaid());
            assertFalse(mockPaymentReturnDto2.isAuthorized());
            assertFalse(mockPaymentReturnDto2.isCanRetry());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void whenCheckReturnUrlWithEmptySettlementThenReturnStatus200() throws Exception {
        /* First create mock payment from mock order to make the whole return url check process possible */
        Payment mockPaymentDto = mockCheckReturnUrlProcess();

        /* Create callback and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(mockPaymentDto.getPaymentId(), mockPaymentDto.getPaytrailTransactionId(), mockStatus);

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, PAYTRAIL_SECRET_KEY);
            mockCallbackCheckoutParams.put("signature", mockSignature);

            /* Convert mock callback checkout params to actual query params */
            String queryParams = mockCallbackCheckoutParams.keySet().stream()
                    .map(key -> key + "=" + mockCallbackCheckoutParams.get(key))
                    .collect(Collectors.joining("&"));
            queryParams += "&" + "merchantId=" + TEST_MERCHANT_ID;
            
            MvcResult checkReturnUrlResponse = this.mockMvc.perform(
                            get("/payment/paytrail/check-return-url?" + queryParams)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(status().is(200))
                    .andReturn();
            PaymentReturnDto mockPaymentReturnDto = mapper.readValue(checkReturnUrlResponse.getResponse().getContentAsString(), PaymentReturnDto.class);

            /* Verify correct payment return dto */
            assertTrue(mockPaymentReturnDto.isValid());
            assertFalse(mockPaymentReturnDto.isPaymentPaid());
            assertFalse(mockPaymentReturnDto.isAuthorized()); // false until subscription flow is fully supported
            assertFalse(mockPaymentReturnDto.isCanRetry());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void whenCheckReturnUrlWithFailStatusThenReturnStatus200() throws Exception {
        /* First create mock payment from mock order to make the whole return url check process possible */
        Payment mockPaymentDto = mockCheckReturnUrlProcess();

        /* Create callback and check url */
        String mockStatus = "fail";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(mockPaymentDto.getPaymentId(), mockPaymentDto.getPaytrailTransactionId(), mockStatus);

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, PAYTRAIL_SECRET_KEY);
            mockCallbackCheckoutParams.put("signature", mockSignature);

            /* Convert mock callback checkout params to actual query params */
            String queryParams = mockCallbackCheckoutParams.keySet().stream()
                    .map(key -> key + "=" + mockCallbackCheckoutParams.get(key))
                    .collect(Collectors.joining("&"));
            queryParams += "&" + "merchantId=" + TEST_MERCHANT_ID;

            MvcResult checkReturnUrlResponse = this.mockMvc.perform(
                            get("/payment/paytrail/check-return-url?" + queryParams)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(status().is(200))
                    .andReturn();
            PaymentReturnDto mockPaymentReturnDto = mapper.readValue(checkReturnUrlResponse.getResponse().getContentAsString(), PaymentReturnDto.class);

            /* Verify correct payment return dto - should be able to retry*/
            assertTrue(mockPaymentReturnDto.isValid());
            assertFalse(mockPaymentReturnDto.isPaymentPaid());
            assertFalse(mockPaymentReturnDto.isAuthorized());
            assertTrue(mockPaymentReturnDto.isCanRetry());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private Payment mockCreateValidPaymentFromOrder(GetPaymentRequestDataDto paymentRequestDataDto, PaytrailPaymentContext mockPaymentContext, String mockPaymentId) {
        OrderWrapper orderWrapper = paymentRequestDataDto.getOrder();
        Payment mockPayment = TestPaymentCreator.createDummyPaymentFromGetPaymentRequest(paymentRequestDataDto, orderWrapper.getOrder().getType(), mockPaymentId, mockPaymentContext.isUseShopInShop());
        PaymentItem mockPaymentItem = TestPaymentCreator.createDummyPaymentItem(mockPaymentId, orderWrapper.getItems().get(0));
        Payer mockPayer = TestPaymentCreator.createDummyPayer(mockPaymentId, orderWrapper.getOrder());

        mockCreatePaymentFlow(mockPaymentContext, mockPayment, mockPaymentItem, mockPayer);

        return mockPayment;
    }

    private void mockCreatePaymentFlow(PaytrailPaymentContext mockPaymentContext, Payment mockPayment, PaymentItem mockPaymentItem, Payer mockPayer) {
        Mockito.when(paymentPaytrailService.getPaymentRequestData(Mockito.any(GetPaymentRequestDataDto.class))).thenCallRealMethod();
        Mockito.when(paymentContextBuilder.buildFor(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(boolean.class))).thenReturn(mockPaymentContext);
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenReturn(mockPayment);
        Mockito.when(paymentItemRepository.save(Mockito.any(PaymentItem.class))).thenReturn(mockPaymentItem);
        Mockito.when(payerRepository.save(Mockito.any(Payer.class))).thenReturn(mockPayer);
    }

    private Payment mockCheckReturnUrlProcess() throws Exception {
        /* First create mock payment from mock order to make the whole return url check process possible */
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setMerchantId(TEST_MERCHANT_ID);
        paymentRequestDataDto.setLanguage("FI");
        paymentRequestDataDto.setPaymentMethod("nordea");
        paymentRequestDataDto.setPaymentMethodLabel("Nordea");

        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String mockPaymentId = PaymentUtil.generatePaymentOrderNumber(orderWrapper.getOrder().getOrderId());
        PaytrailPaymentContext mockPaymentContext = createMockPaytrailPaymentContext(orderWrapper.getOrder().getNamespace(), paymentRequestDataDto.getMerchantId());
        mockCreateValidPaymentFromOrder(paymentRequestDataDto, mockPaymentContext, mockPaymentId);

        MvcResult createPaymentResponse = this.mockMvc.perform(
                        post("/payment/paytrail/createFromOrder")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(paymentRequestDataDto))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(201))
                .andReturn();
        Payment mockPaymentDto = mapper.readValue(createPaymentResponse.getResponse().getContentAsString(), Payment.class);

        /* Mock check return url flow with mock payment*/
        mockCheckReturnUrlFlow(mockPaymentDto);

        return mockPaymentDto;
    }

    private void mockCheckReturnUrlFlow(Payment mockPayment) {
        Mockito.when(onlinePaymentService.getPayment(Mockito.any(String.class))).thenReturn(mockPayment);
        Mockito.when(paytrailPaymentReturnValidator.validateChecksum(Mockito.any(Map.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class))).thenCallRealMethod();
        Mockito.when(paymentRepository.findById(Mockito.any(String.class))).thenReturn(Optional.of(mockPayment));
        Mockito.when(commonServiceConfigurationClient.getMerchantPaytrailSecretKey(Mockito.any(String.class))).thenReturn(PAYTRAIL_SECRET_KEY);
        Mockito.when(paytrailPaymentReturnValidator.validateReturnValues(Mockito.any(boolean.class), Mockito.any(String.class), Mockito.nullable(String.class))).thenCallRealMethod();
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

    private PaytrailPaymentContext createMockPaytrailPaymentContext(String namespace, String merchantId) {
        PaytrailPaymentContext mockPaymentContext = new PaytrailPaymentContext();
        mockPaymentContext.setNamespace(namespace);
        mockPaymentContext.setInternalMerchantId(merchantId);
        mockPaymentContext.setPaytrailMerchantId(PAYTRAIL_MERCHANT_ID);
        mockPaymentContext.setPaytrailSecretKey(PAYTRAIL_SECRET_KEY);
        mockPaymentContext.setDefaultCurrency("EUR");
        mockPaymentContext.setDefaultLanguage("FI");
        mockPaymentContext.setReturnUrl("https://ecom.example.com/cart");
        mockPaymentContext.setNotifyUrl("https://ecom.example.com/cart");
        mockPaymentContext.setCp("PRO-31312-1");

        return mockPaymentContext;
    }



    private Map<String, String> createMockCallbackParams(String paymentId, String transactionId, String status) {
        Map<String, String> mockCallbackCheckoutParams = new HashMap<>();
        mockCallbackCheckoutParams.put("checkout-account", PAYTRAIL_MERCHANT_ID);
        mockCallbackCheckoutParams.put("checkout-algorithm", "sha256");
        mockCallbackCheckoutParams.put("checkout-amount", "2964");
        mockCallbackCheckoutParams.put("checkout-stamp", paymentId);
        mockCallbackCheckoutParams.put("checkout-reference", "192387192837195");
        mockCallbackCheckoutParams.put("checkout-transaction-id", transactionId);
        mockCallbackCheckoutParams.put("checkout-status", status);
        mockCallbackCheckoutParams.put("checkout-provider", "nordea");

        return mockCallbackCheckoutParams;
    }

}
