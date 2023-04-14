package fi.hel.verkkokauppa.payment.paytrail.context;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.dto.configuration.ConfigurationDto;
import fi.hel.verkkokauppa.common.rest.dto.configuration.MerchantDto;
import fi.hel.verkkokauppa.payment.testing.annotations.UnitTest;
import fi.hel.verkkokauppa.payment.testing.utils.AutoMockBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;


@UnitTest
@SpringBootTest(classes = PaytrailPaymentContextBuilder.class)
@ContextConfiguration(classes = {AutoMockBeanFactory.class})
@Slf4j
@TestPropertySource(properties = {
        "paytrail_payment_return_success_url=return_success_url",
        "paytrail_payment_return_cancel_url=return_cancel_url",
        "paytrail_payment_notify_success_url=notify_success_url",
        "paytrail_payment_notify_cancel_url=notify_cancel_url",
        "paytrail_card_redirect_success_url=url1",
        "paytrail_card_redirect_cancel_url=url2",
        "paytrail_card_callback_success_url=url3",
        "paytrail_card_callback_cancel_url=url4",
        "paytrail_update_card_redirect_success_url=update/url1",
        "paytrail_update_card_redirect_cancel_url=update/url2",
        "paytrail_update_card_callback_success_url=update/url3",
        "paytrail_update_card_callback_cancel_url=update/url4"
})
public class PaytrailPaymentContextBuilderUnitTests {
    private static final String PAYTRAIL_MERCHANT_ID = "375917";
    private static final String PAYTRAIL_SECRET_KEY = "SAIPPUAKAUPPIAS";
    private static final String NAMESPACE = "namespace";
    private static final String MERCHANT_ID = "merchantId";

    @Autowired
    private PaytrailPaymentContextBuilder paymentContextBuilder;

    @Autowired
    private Environment env;

    @MockBean
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(paymentContextBuilder, "commonServiceConfigurationClient", commonServiceConfigurationClient);
    }

    @Test
    public void testBuildContextSuccess() throws Exception {
        Mockito.when(commonServiceConfigurationClient.getMerchantModel(any(), any()))
                .thenReturn(createMockMerchantDto(NAMESPACE, MERCHANT_ID, PAYTRAIL_MERCHANT_ID));
        Mockito.when(commonServiceConfigurationClient.getMerchantPaytrailSecretKey(any())).thenReturn(PAYTRAIL_SECRET_KEY);

        PaytrailPaymentContext context = paymentContextBuilder.buildFor(NAMESPACE, MERCHANT_ID, false);

        assertEquals(MERCHANT_ID, context.getInternalMerchantId());
        assertEquals(NAMESPACE, context.getNamespace());

        assertEquals(PAYTRAIL_MERCHANT_ID, context.getPaytrailMerchantId());
        assertEquals(PAYTRAIL_SECRET_KEY, context.getPaytrailSecretKey());

        assertEquals("return_success_url", context.getRedirectSuccessUrl());
        assertEquals("return_cancel_url", context.getRedirectCancelUrl());
        assertEquals("notify_success_url", context.getCallbackSuccessUrl());
        assertEquals("notify_cancel_url", context.getCallbackCancelUrl());

        assertEquals("url1", context.getCardRedirectSuccessUrl());
        assertEquals("url2", context.getCardRedirectCancelUrl());
        assertEquals("url3", context.getCardCallbackSuccessUrl());
        assertEquals("url4", context.getCardCallbackCancelUrl());

        assertEquals("update/url1", context.getUpdateCardRedirectSuccessUrl());
        assertEquals("update/url2", context.getUpdateCardRedirectCancelUrl());
        assertEquals("update/url3", context.getUpdateCardCallbackSuccessUrl());
        assertEquals("update/url4", context.getUpdateCardCallbackCancelUrl());
    }

    @Test
    public void testBuildContextWithMissingPaytrailMerchantId() throws Exception {
        Mockito.when(commonServiceConfigurationClient.getMerchantModel(any(), any()))
                .thenReturn(createMockMerchantDto(NAMESPACE, MERCHANT_ID, null));
        Mockito.when(commonServiceConfigurationClient.getMerchantPaytrailSecretKey(any())).thenReturn(PAYTRAIL_SECRET_KEY);

        Exception exception = assertThrows(Exception.class, () -> {
            paymentContextBuilder.buildFor(NAMESPACE, MERCHANT_ID, false);
        });
        CommonApiException cae = (CommonApiException) exception;
        assertEquals(CommonApiException.class, cae.getClass());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, cae.getStatus());
        assertEquals("failed-to-get-paytrail-merchant-id", cae.getErrors().getErrors().get(0).getCode());
    }

    @Test
    public void testBuildContextWithMissingPaytrailSecret() throws Exception {
        Mockito.when(commonServiceConfigurationClient.getMerchantModel(any(), any()))
                .thenReturn(createMockMerchantDto(NAMESPACE, MERCHANT_ID, PAYTRAIL_MERCHANT_ID));
        Mockito.when(commonServiceConfigurationClient.getMerchantPaytrailSecretKey(any())).thenReturn(null);

        Exception exception = assertThrows(Exception.class, () -> {
            paymentContextBuilder.buildFor(NAMESPACE, MERCHANT_ID, false);
        });
        CommonApiException cae = (CommonApiException) exception;
        assertEquals(CommonApiException.class, cae.getClass());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, cae.getStatus());
        assertEquals("failed-to-get-merchant-paytrail-secret-key", cae.getErrors().getErrors().get(0).getCode());
    }

    private MerchantDto createMockMerchantDto(String namespace, String merchantId, String paytrailMerchantId) {
        MerchantDto mockMerchantDto = new MerchantDto();
        mockMerchantDto.setNamespace(namespace);
        mockMerchantDto.setMerchantId(merchantId);
        mockMerchantDto.setCreatedAt(LocalDateTime.now());
        mockMerchantDto.setUpdatedAt(LocalDateTime.now());

        ConfigurationDto mockConfigurationDto = new ConfigurationDto();
        mockConfigurationDto.setKey(ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID);
        mockConfigurationDto.setValue(paytrailMerchantId);

        ArrayList<ConfigurationDto> mockConfigurations = new ArrayList<>();
        mockConfigurations.add(mockConfigurationDto);
        mockMerchantDto.setConfigurations(mockConfigurations);

        return mockMerchantDto;
    }

}
