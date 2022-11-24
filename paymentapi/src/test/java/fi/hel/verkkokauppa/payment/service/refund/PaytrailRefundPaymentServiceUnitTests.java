package fi.hel.verkkokauppa.payment.service.refund;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.model.refund.RefundGateway;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.model.refund.RefundPaymentStatus;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailPaymentClient;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.impl.PaytrailCreateRefundPayloadConverter;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import fi.hel.verkkokauppa.payment.repository.refund.RefundPaymentRepository;
import fi.hel.verkkokauppa.payment.service.refund.PaytrailRefundPaymentService;
import fi.hel.verkkokauppa.payment.testing.annotations.UnitTest;
import fi.hel.verkkokauppa.payment.testing.utils.AutoMockBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailRefundCreateResponseMapper;
import org.helsinki.paytrail.model.refunds.PaytrailRefundResponse;
import org.helsinki.paytrail.response.PaytrailResponse;
import org.helsinki.paytrail.response.refunds.PaytrailRefundCreateResponse;
import org.helsinki.paytrail.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@UnitTest
@WebMvcTest(PaytrailRefundPaymentService.class)
@ContextConfiguration(classes = AutoMockBeanFactory.class) // This automatically mocks missing beans
@Slf4j
public class PaytrailRefundPaymentServiceUnitTests {

    private static final String PAYTRAIL_MERCHANT_ID = "375917";
    private static final String PAYTRAIL_SECRET_KEY = "SAIPPUAKAUPPIAS";

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PaytrailRefundPaymentService refundPaymentService;

    @MockBean
    PaytrailPaymentClient paytrailPaymentClient;

    @MockBean
    PaytrailAuthClientFactory paytrailAuthClientFactory;

    @MockBean
    PaytrailCreateRefundPayloadConverter refundPayloadConverter;
    @MockBean
    PaytrailRefundCreateResponseMapper refundCreateResponseMapper;

    @MockBean
    RefundPaymentRepository refundPaymentRepository;

    @MockBean
    PaytrailClient paytrailClient;

    @Mock
    Environment env;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(paytrailPaymentClient, "paytrailAuthClientFactory", paytrailAuthClientFactory);
        ReflectionTestUtils.setField(paytrailPaymentClient, "refundPayloadConverter", refundPayloadConverter);
        ReflectionTestUtils.setField(refundPaymentService, "paytrailPaymentClient", paytrailPaymentClient);
        ReflectionTestUtils.setField(refundPayloadConverter, "env", env);
        ReflectionTestUtils.setField(paytrailPaymentClient, "refundCreateResponseMapper", refundCreateResponseMapper);
        ReflectionTestUtils.setField(refundPaymentService, "refundPaymentRepository", refundPaymentRepository);
    }

    @Test
    public void createRefundToPaytrailAndCreateRefundPayment() throws JsonProcessingException {
        String orderId = "dummy-order-id";

        RefundRequestDataDto dto = new RefundRequestDataDto();
        RefundAggregateDto refundAggregateDto = new RefundAggregateDto();
        RefundDto refundDto = new RefundDto();
        String refundId = "refund-id";
        refundDto.setRefundId(refundId);
        refundDto.setStatus("confirmed");
        String user = "dummy_user";
        refundDto.setUser(user);
        String namespace = "venepaikat";
        refundDto.setNamespace(namespace);
        // TODO refund dto values to createRefundPayment

        refundDto.setOrderId(orderId);
        refundDto.setPriceNet("10");
        refundDto.setPriceVat("0");
        refundDto.setPriceTotal("10");
        refundDto.setCustomerEmail(UUID.randomUUID() + "@ambientia.fi");

        refundAggregateDto.setRefund(refundDto);
        ArrayList<RefundItemDto> refundItemDtos = new ArrayList<>();
        RefundItemDto itemDto = new RefundItemDto();

        String itemMerchantId = "itemMerchantId";
        itemDto.setMerchantId(itemMerchantId);
        refundItemDtos.add(itemDto);
        refundAggregateDto.setItems(refundItemDtos);

        OrderWrapper orderWrapper = new OrderWrapper();
        OrderDto order = new OrderDto();
        order.setType(OrderType.ORDER);
        orderWrapper.setOrder(order);

        PaymentDto paymentDto = new PaymentDto();

        paymentDto.setShopInShopPayment(false);
        paymentDto.setPaytrailTransactionId("mock-transaction-id");
        String paymentMethod = "payment-method";
        paymentDto.setPaymentMethod(paymentMethod);

        dto.setPayment(paymentDto);
        dto.setOrder(orderWrapper);
        dto.setRefund(refundAggregateDto);


        PaytrailPaymentContext mockPaymentContext = createMockPaytrailPaymentContext(namespace, itemMerchantId);

        when(refundPaymentService.createRefundToPaytrailAndCreateRefundPayment(any())).thenCallRealMethod();

        when(refundPaymentService.createPaytrailRefundContext(any(), any(), any())).thenReturn(
                mockPaymentContext
        );

        when(paytrailPaymentClient.createRefund(any(), any(), any(), any())).thenCallRealMethod();

        when(paytrailPaymentClient.createPaytrailClientFromPaymentContext(any())).thenCallRealMethod();

        when(paytrailAuthClientFactory.getClient(any(), any())).thenReturn(paytrailClient);

        when(refundPayloadConverter.convertToPayload(any(), any(), any())).thenCallRealMethod();

        when(env.getRequiredProperty(any())).thenReturn("http://localhost:3000/");

        CompletableFuture<Pair<PaytrailResponse, String>> responseFuture = new CompletableFuture<>();
        PaytrailResponse paytrailResponse = new PaytrailResponse();
        paytrailResponse.setValid(true);
        Executors.newCachedThreadPool().submit(() -> {
            Thread.sleep(500);
            responseFuture.complete(new Pair<>(paytrailResponse, "{}"));
            return null;
        });

        Mockito.when(paytrailClient.sendRequest(Mockito.any())).thenReturn(responseFuture.thenApply(result -> paytrailResponse));

        when(paytrailClient.getBaseUrl()).thenReturn("http://localhost:3000/");

        PaytrailRefundCreateResponse refundCreateResponse = new PaytrailRefundCreateResponse();
        PaytrailRefundResponse refundResponse = new PaytrailRefundResponse();
        refundResponse.setValid(true);
        refundResponse.setTransactionId("setTransactionId");
        refundCreateResponse.setRefundResponse(refundResponse);
        when(refundCreateResponseMapper.to(any())).thenReturn(refundCreateResponse);

        // Mocking the `save` method of the `refundPaymentRepository` to return the same object that was passed to it
        Mockito.when(refundPaymentRepository.save(any(RefundPayment.class))).thenAnswer(i -> i.getArguments()[0]);

        log.info(objectMapper.writeValueAsString(dto));
        RefundPayment refundPayment = refundPaymentService.createRefundToPaytrailAndCreateRefundPayment(dto);

        Assertions.assertNotEquals(refundDto.getRefundId(), refundPayment.getRefundPaymentId());
        Assertions.assertNotNull(refundPayment.getRefundPaymentId());
        Assertions.assertNotNull(refundPayment.getRefundTransactionId());
        Assertions.assertNotNull(refundPayment.getCreatedAt());
        Assertions.assertEquals(namespace, refundPayment.getNamespace());
        Assertions.assertEquals(orderId, refundPayment.getOrderId());
        Assertions.assertEquals(user, refundPayment.getUserId());
        Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());
        Assertions.assertEquals(paymentMethod, refundPayment.getRefundMethod());
        Assertions.assertEquals(new BigDecimal(refundDto.getPriceNet()), refundPayment.getTotalExclTax());
        Assertions.assertEquals(new BigDecimal(refundDto.getPriceTotal()), refundPayment.getTotal());
        Assertions.assertEquals(new BigDecimal(refundDto.getPriceVat()), refundPayment.getTaxAmount());
        Assertions.assertEquals(RefundGateway.PAYTRAIL, refundPayment.getRefundGateway());

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

}
