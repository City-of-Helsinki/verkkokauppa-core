package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.payment.api.PaytrailPaymentController;
import fi.hel.verkkokauppa.payment.mapper.PaytrailCreatePaymentPayloadMapper;
import fi.hel.verkkokauppa.payment.mapper.PaytrailPaymentProviderListMapper;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailPaymentClient;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailPaymentStatusClient;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.paytrail.converter.IPaytrailPayloadConverter;
import fi.hel.verkkokauppa.payment.paytrail.validation.PaytrailPaymentReturnValidator;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.testing.annotations.UnitTest;
import fi.hel.verkkokauppa.payment.testing.utils.AutoMockBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest;
import org.helsinki.paytrail.request.refunds.PaytrailRefundCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;


@UnitTest
@SpringBootTest(classes = PaymentPaytrailService.class)
@ContextConfiguration(classes = {AutoMockBeanFactory.class})
@Slf4j
public class PaytrailPaymentServiceUnitTests {
    private static final String PAYTRAIL_MERCHANT_ID = "375917";
    private static final String PAYTRAIL_SECRET_KEY = "SAIPPUAKAUPPIAS";

    @Autowired
    private PaymentPaytrailService paymentPaytrailService;

    @Autowired
    private Environment env;

    @MockBean
    private OnlinePaymentService onlinePaymentService;

    @MockBean
    private ObjectMapper mapper;

    @MockBean
    private PaytrailPaymentReturnValidator paytrailPaymentReturnValidator;

    @MockBean
    private PaytrailPaymentContextBuilder paymentContextBuilder;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private PayerRepository payerRepository;

    @MockBean
    private PaymentItemRepository paymentItemRepository;

    @MockBean
    private PaytrailPaymentProviderListMapper paytrailPaymentProviderListMapper;

    @MockBean
    private PaytrailPaymentController paytrailPaymentController;

    @MockBean
    private IPaytrailPayloadConverter<PaytrailPaymentCreateRequest.CreatePaymentPayload, OrderMessage> createPaymentPayloadFromOrderMessage;

    @MockBean
    private IPaytrailPayloadConverter<PaytrailRefundCreateRequest.CreateRefundPayload, RefundDto> paytrailCreateRefundPayloadConverter;

    @MockBean
    private PaytrailCreatePaymentPayloadMapper paytrailCreatePaymentPayloadMapper;

    @MockBean
    private SendEventService sendEventService;

    @MockBean
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

    @MockBean
    private PaytrailPaymentClient paytrailPaymentClient;

    @MockBean
    private SendNotificationService sendNotificationService;

    @MockBean
    private RestServiceClient restServiceClient;
    @MockBean
    private PaytrailPaymentStatusClient paytrailPaymentStatusClient;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(paymentPaytrailService, "paymentContextBuilder", paymentContextBuilder);
    }

    @Test
    public void testGetCardFormParameters() throws Exception {
        Mockito.when(paymentContextBuilder.buildFor(any(), any(), eq(false))).thenReturn(createMockPaytrailPaymentContext("namespace", "merchantId"));

        Map<?, ?> parameters = paymentPaytrailService.getCardReturnParameters("merchantId", "namespace", "orderId");

        assertEquals(10, parameters.size());
        assertEquals(parameters.get("checkout-account"), PAYTRAIL_MERCHANT_ID);
        assertEquals(parameters.get("checkout-algorithm"), "sha256");
        assertEquals(parameters.get("checkout-method"), "POST");
        assertNotNull(parameters.get("checkout-nonce"));
        assertNotNull(parameters.get("checkout-timestamp"));
        assertEquals("CardRedirectSuccessUrl/orderId", parameters.get("checkout-redirect-success-url"));
        assertEquals("CardRedirectCancelUrl/orderId", parameters.get("checkout-redirect-cancel-url"));
        assertEquals("CardCallbackSuccessUrl/orderId", parameters.get("checkout-callback-success-url"));
        assertEquals("CardCallbackCancelUrl/orderId", parameters.get("checkout-callback-cancel-url"));
        assertNotNull(parameters.get("signature"));
    }

    @Test
    public void testGetUpdateCardFormParameters() throws Exception {
        Mockito.when(paymentContextBuilder.buildFor(any(), any(), eq(false))).thenReturn(createMockPaytrailPaymentContext("namespace", "merchantId"));

        Map<?, ?> parameters = paymentPaytrailService.getUpdateCardReturnParameters("merchantId", "namespace", "orderId");

        assertEquals(10, parameters.size());
        assertEquals(parameters.get("checkout-account"), PAYTRAIL_MERCHANT_ID);
        assertEquals(parameters.get("checkout-algorithm"), "sha256");
        assertEquals(parameters.get("checkout-method"), "POST");
        assertNotNull(parameters.get("checkout-nonce"));
        assertNotNull(parameters.get("checkout-timestamp"));
        assertEquals("UpdateCardRedirectSuccessUrl/orderId", parameters.get("checkout-redirect-success-url"));
        assertEquals("UpdateCardRedirectCancelUrl/orderId", parameters.get("checkout-redirect-cancel-url"));
        assertEquals("UpdateCardCallbackSuccessUrl/orderId", parameters.get("checkout-callback-success-url"));
        assertEquals("UpdateCardCallbackCancelUrl/orderId", parameters.get("checkout-callback-cancel-url"));
        assertNotNull(parameters.get("signature"));
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

        mockPaymentContext.setCardRedirectSuccessUrl("CardRedirectSuccessUrl");
        mockPaymentContext.setCardRedirectCancelUrl("CardRedirectCancelUrl");
        mockPaymentContext.setCardCallbackSuccessUrl("CardCallbackSuccessUrl");
        mockPaymentContext.setCardCallbackCancelUrl("CardCallbackCancelUrl");

        mockPaymentContext.setUpdateCardRedirectSuccessUrl("UpdateCardRedirectSuccessUrl");
        mockPaymentContext.setUpdateCardRedirectCancelUrl("UpdateCardRedirectCancelUrl");
        mockPaymentContext.setUpdateCardCallbackSuccessUrl("UpdateCardCallbackSuccessUrl");
        mockPaymentContext.setUpdateCardCallbackCancelUrl("UpdateCardCallbackCancelUrl");
        return mockPaymentContext;
    }

}
