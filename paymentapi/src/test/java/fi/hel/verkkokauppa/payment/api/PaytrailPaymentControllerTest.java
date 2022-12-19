package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.rest.dto.configuration.ConfigurationDto;
import fi.hel.verkkokauppa.common.rest.dto.configuration.MerchantDto;
import fi.hel.verkkokauppa.common.util.ConfigurationParseUtil;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.testing.BaseFunctionalTest;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class PaytrailPaymentControllerTest extends BaseFunctionalTest {

    private static final String TEST_PAYTRAIL_SHOP_IN_SHOP_ID = "695874";
    private static final String TEST_PAYTRAIL_MERCHANT_ID = "375917";
    private static final String TEST_PAYTRAIL_SECRET_KEY = "SAIPPUAKAUPPIAS";

    @Autowired
    private PaytrailPaymentController paytrailPaymentController;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PayerRepository payerRepository;

    @Autowired
    private PaymentItemRepository paymentItemRepository;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ServiceUrls serviceUrls;

    @Autowired
    private ObjectMapper mapper;

    @Value("${paytrail.aggregate.merchant.id}")
    private String aggregateMerchantId;

    @Value("${paytrail.aggregate.merchant.secret}")
    private String aggregateSecretKey;

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
        paymentRequestDataDto.setPaymentMethod("nordea");
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        OrderItemDto dummyOrderItem = orderWrapper.getItems().get(0);
        paymentRequestDataDto.setOrder(orderWrapper);

        paymentRequestDataDto.setPaymentMethod("nordea");
        String merchantId = getFirstMerchantIdFromNamespace(orderWrapper.getOrder().getNamespace());
        paymentRequestDataDto.setMerchantId(merchantId);

        ResponseEntity<Payment> paymentResponse = paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        Payment payment = paymentResponse.getBody();

        if (payment.getPaymentId() != null) {
            /* Check payment */
            assertEquals(orderWrapper.getOrder().getOrderId(), payment.getOrderId());
            assertNotNull(payment.getPaytrailTransactionId());

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
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        orderWrapper.getOrder().setUser("");
        paymentRequestDataDto.setOrder(orderWrapper);

        String merchantId = getFirstMerchantIdFromNamespace(orderWrapper.getOrder().getNamespace());
        paymentRequestDataDto.setMerchantId(merchantId);

        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        });
        assertEquals(CommonApiException.class, exception.getClass());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("rejected-creating-paytrail-payment-for-order-without-user", exception.getErrors().getErrors().get(0).getCode());
        assertEquals("rejected creating paytrail payment for order without user, order id [" + orderWrapper.getOrder().getOrderId() + "]", exception.getErrors().getErrors().get(0).getMessage());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCreatePaymentFromOrderWithInvalidStatus() {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();

        /* Test with OrderStatus.DRAFT */
        OrderWrapper orderWrapper1 = createDummyOrderWrapper();
        orderWrapper1.getOrder().setStatus("draft");
        paymentRequestDataDto.setOrder(orderWrapper1);

        String merchantId = getFirstMerchantIdFromNamespace(orderWrapper1.getOrder().getNamespace());
        paymentRequestDataDto.setMerchantId(merchantId);

        CommonApiException exception1 = assertThrows(CommonApiException.class, () -> {
            paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        });
        assertEquals(CommonApiException.class, exception1.getClass());
        assertEquals(HttpStatus.FORBIDDEN, exception1.getStatus());
        assertEquals("rejected-creating-paytrail-payment-for-unconfirmed-order", exception1.getErrors().getErrors().get(0).getCode());
        assertEquals("rejected creating paytrail payment for unconfirmed order, order id [" + orderWrapper1.getOrder().getOrderId() + "]", exception1.getErrors().getErrors().get(0).getMessage());

        /* Test with OrderStatus.CANCELLED */
        OrderWrapper orderWrapper2 = createDummyOrderWrapper();
        orderWrapper2.getOrder().setStatus("cancelled");
        paymentRequestDataDto.setOrder(orderWrapper2);

        CommonApiException exception2 = assertThrows(CommonApiException.class, () -> {
            paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        });
        assertEquals(CommonApiException.class, exception2.getClass());
        assertEquals(HttpStatus.FORBIDDEN, exception2.getStatus());
        assertEquals("rejected-creating-paytrail-payment-for-unconfirmed-order", exception2.getErrors().getErrors().get(0).getCode());
        assertEquals("rejected creating paytrail payment for unconfirmed order, order id [" + orderWrapper2.getOrder().getOrderId() + "]", exception2.getErrors().getErrors().get(0).getMessage());
    }

    /*
     * PaytrailPaymentContext is at the moment always built for normal merchant flow, so this test is not yet working.
     * This kind of test is needed, when there is full support implemented for both shop-in-shop and normal merchant flows.
     * @Test
     * @RunIfProfile(profile = "local")
     *
     */
    public void testCreatePaymentFromOrderWithInvalidShopInShopPaymentContextMissingShopId() {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String namespace = orderWrapper.getOrder().getNamespace();
        String merchantId = getFirstMerchantIdFromNamespace(namespace);
        paymentRequestDataDto.setMerchantId(merchantId);

        /* Temporarily invalidate merchant specifid paytrail merchant shop ID */
        MerchantDto merchantConfigWithInvalidShopId = updateMerchantConfigurationValueByKey(merchantId, namespace, ServiceConfigurationKeys.MERCHANT_SHOP_ID, "");
        ConfigurationDto shopIdConfig = ConfigurationParseUtil.parseConfigurationValueByKey(merchantConfigWithInvalidShopId.getConfigurations(), ServiceConfigurationKeys.MERCHANT_SHOP_ID).get();
        assertEquals("", shopIdConfig.getValue());



        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        });
        assertEquals(CommonApiException.class, exception.getClass());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("validation-failed-for-paytrail-payment-context-without-merchant-shop-id", exception.getErrors().getErrors().get(0).getCode());
        assertEquals("Failed to validate paytrail payment context, merchant shop id not found for merchant [" + merchantId + "]", exception.getErrors().getErrors().get(0).getMessage());

        /* Set back Paytrail test shop ID */
        MerchantDto merchantConfigWithValidShopId = updateMerchantConfigurationValueByKey(merchantId, namespace, ServiceConfigurationKeys.MERCHANT_SHOP_ID, TEST_PAYTRAIL_SHOP_IN_SHOP_ID);
        ConfigurationDto validShopIdConfig = ConfigurationParseUtil.parseConfigurationValueByKey(merchantConfigWithValidShopId.getConfigurations(), ServiceConfigurationKeys.MERCHANT_SHOP_ID).get();
        assertEquals(TEST_PAYTRAIL_SHOP_IN_SHOP_ID, validShopIdConfig.getValue());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCreatePaymentFromOrderWithInvalidNormalPaymentContextMissingCredentials() {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String namespace = orderWrapper.getOrder().getNamespace();
        String merchantId = getFirstMerchantIdFromNamespace(namespace);
        paymentRequestDataDto.setMerchantId(merchantId);

        /* Temporarily invalidate merchant specifid paytrail normal merchant ID */
        MerchantDto merchantConfigWithInvalidPaytrailMerchantId = updateMerchantConfigurationValueByKey(merchantId, namespace, ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID, "");
        ConfigurationDto paytrailMerchantIdConfig = ConfigurationParseUtil.parseConfigurationValueByKey(merchantConfigWithInvalidPaytrailMerchantId.getConfigurations(), ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID).get();
        assertEquals("", paytrailMerchantIdConfig.getValue());

        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        });
        assertEquals(CommonApiException.class, exception.getClass());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("validation-failed-for-paytrail-payment-context-without-paytrail-merchant-credentials", exception.getErrors().getErrors().get(0).getCode());
        assertEquals("Failed to validate paytrail payment context, merchant credentials (merchant ID or secret key) are missing for merchant [" + merchantId + "]", exception.getErrors().getErrors().get(0).getMessage());

        /* Set back Paytrail test shop ID */
        MerchantDto merchantConfigWithValidPaytrailMerchantId = updateMerchantConfigurationValueByKey(merchantId, namespace, ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID, TEST_PAYTRAIL_MERCHANT_ID);
        ConfigurationDto validPaytrailMerchantIdConfig = ConfigurationParseUtil.parseConfigurationValueByKey(merchantConfigWithValidPaytrailMerchantId.getConfigurations(), ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID).get();
        assertEquals(TEST_PAYTRAIL_MERCHANT_ID, validPaytrailMerchantIdConfig.getValue());
    }


    @Test
    @RunIfProfile(profile = "local")
    public void testCheckReturnUrl() {
        /* First create payment from mock order to make the whole return url check process possible*/
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setPaymentMethod("nordea");
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String merchantId = getFirstMerchantIdFromNamespace(orderWrapper.getOrder().getNamespace());
        paymentRequestDataDto.setMerchantId(merchantId);

        ResponseEntity<Payment> paymentResponse = paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        Payment payment = paymentResponse.getBody();
        assertEquals(PaymentStatus.CREATED, payment.getStatus());

        /* Create callback and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(payment.getPaymentId(), payment.getPaytrailTransactionId(), mockStatus);

        String mockSettlementReference = "8739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams.put("checkout-settlement-reference", mockSettlementReference);

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, TEST_PAYTRAIL_SECRET_KEY);
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<PaymentReturnDto> response = paytrailPaymentController.checkReturnUrl(merchantId, mockSignature, mockStatus, payment.getPaymentId(), mockSettlementReference, mockCallbackCheckoutParams);
            PaymentReturnDto paymentReturnDto = response.getBody();

            /* Verify correct payment return dto */
            assertTrue(paymentReturnDto.isValid());
            assertTrue(paymentReturnDto.isPaymentPaid());
            assertFalse(paymentReturnDto.isAuthorized());
            assertFalse(paymentReturnDto.isCanRetry());

            /* Verify updated payment */
            Payment updatedPayment = paymentRepository.findById(payment.getPaymentId()).get();
            assertEquals(PaymentStatus.PAID_ONLINE, updatedPayment.getStatus());
            assertEquals(paymentReturnDto.getPaymentType(), updatedPayment.getPaymentType());


        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        toBeDeletedPaymentId.add(payment.getPaymentId());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCheckReturnUrlWithInvalidSignature() {
        /* First create payment from mock order to make the whole return url check process possible*/
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setPaymentMethod("nordea");
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String merchantId = getFirstMerchantIdFromNamespace(orderWrapper.getOrder().getNamespace());
        paymentRequestDataDto.setMerchantId(merchantId);

        ResponseEntity<Payment> paymentResponse = paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        Payment payment = paymentResponse.getBody();
        assertEquals(PaymentStatus.CREATED, payment.getStatus());

        /* Create callback and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(payment.getPaymentId(), payment.getPaytrailTransactionId(), mockStatus);
        String mockSettlementReference = "8739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams.put("checkout-settlement-reference", mockSettlementReference);

        String mockSignature = "invalid-739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams.put("signature", mockSignature);

        ResponseEntity<PaymentReturnDto> response = paytrailPaymentController.checkReturnUrl(merchantId, mockSignature, mockStatus, payment.getPaymentId(), mockSettlementReference, mockCallbackCheckoutParams);
        PaymentReturnDto paymentReturnDto = response.getBody();

        /* Verify correct payment return dto - should be invalid, because signature mismatches */
        assertFalse(paymentReturnDto.isValid());
        assertTrue(paymentReturnDto.isPaymentPaid());
        assertFalse(paymentReturnDto.isAuthorized());
        assertFalse(paymentReturnDto.isCanRetry());

        /* Verify that payment status is not changed CREATED */
        Payment updatedPayment = paymentRepository.findById(payment.getPaymentId()).get();
        assertEquals(PaymentStatus.CREATED, updatedPayment.getStatus());
        assertEquals(paymentReturnDto.getPaymentType(), updatedPayment.getPaymentType());

        toBeDeletedPaymentId.add(payment.getPaymentId());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCheckReturnUrlWithEmptySettlement() {
        /* First create payment from mock order to make the whole return url check process possible*/
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setPaymentMethod("nordea");
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String merchantId = getFirstMerchantIdFromNamespace(orderWrapper.getOrder().getNamespace());
        paymentRequestDataDto.setMerchantId(merchantId);

        ResponseEntity<Payment> paymentResponse = paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        Payment payment = paymentResponse.getBody();
        assertEquals(PaymentStatus.CREATED, payment.getStatus());

        /* Create callback and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(payment.getPaymentId(), payment.getPaytrailTransactionId(), mockStatus);

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, TEST_PAYTRAIL_SECRET_KEY);
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<PaymentReturnDto> response = paytrailPaymentController.checkReturnUrl(merchantId, mockSignature, mockStatus, payment.getPaymentId(), null, mockCallbackCheckoutParams);
            PaymentReturnDto paymentReturnDto = response.getBody();

            /* Verify correct payment return dto - should be authorized and paid */
            assertTrue(paymentReturnDto.isValid());
            assertTrue(paymentReturnDto.isPaymentPaid());
            assertFalse(paymentReturnDto.isAuthorized()); // false until subscription flow is fully supported
            assertFalse(paymentReturnDto.isCanRetry());

            /* Verify that payment status is CANCELLED */
            Payment updatedPayment = paymentRepository.findById(payment.getPaymentId()).get();
            assertEquals(PaymentStatus.CANCELLED, updatedPayment.getStatus());
            assertEquals(paymentReturnDto.getPaymentType(), updatedPayment.getPaymentType());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        toBeDeletedPaymentId.add(payment.getPaymentId());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCheckReturnUrlWithFailStatus() {
        /* First create payment from mock order to make the whole return url check process possible */
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
        paymentRequestDataDto.setPaymentMethod("nordea");
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        paymentRequestDataDto.setOrder(orderWrapper);

        String merchantId = getFirstMerchantIdFromNamespace(orderWrapper.getOrder().getNamespace());
        paymentRequestDataDto.setMerchantId(merchantId);

        ResponseEntity<Payment> paymentResponse = paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        Payment payment = paymentResponse.getBody();
        assertEquals(PaymentStatus.CREATED, payment.getStatus());

        /* Create callback and check url */
        String mockStatus = "fail";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(payment.getPaymentId(), payment.getPaytrailTransactionId(), mockStatus);

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, TEST_PAYTRAIL_SECRET_KEY);
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<PaymentReturnDto> response = paytrailPaymentController.checkReturnUrl(merchantId, mockSignature, mockStatus, payment.getPaymentId(), null, mockCallbackCheckoutParams);
            PaymentReturnDto paymentReturnDto = response.getBody();

            /* Verify correct payment return dto - should be only valid and retryable */
            assertTrue(paymentReturnDto.isValid());
            assertFalse(paymentReturnDto.isPaymentPaid());
            assertFalse(paymentReturnDto.isAuthorized());
            assertTrue(paymentReturnDto.isCanRetry());

            /* Verify that payment status is still CREATED */
            Payment updatedPayment = paymentRepository.findById(payment.getPaymentId()).get();
            assertEquals(PaymentStatus.CREATED, updatedPayment.getStatus());
            assertEquals(paymentReturnDto.getPaymentType(), updatedPayment.getPaymentType());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        toBeDeletedPaymentId.add(payment.getPaymentId());
    }

    private Map<String, String> createMockCallbackParams(String paymentId, String transactionId, String status) {
        Map<String, String> mockCallbackCheckoutParams = new HashMap<>();
        mockCallbackCheckoutParams.put("checkout-account", TEST_PAYTRAIL_MERCHANT_ID);
        mockCallbackCheckoutParams.put("checkout-algorithm", "sha256");
        mockCallbackCheckoutParams.put("checkout-amount", "2964");
        mockCallbackCheckoutParams.put("checkout-stamp", paymentId);
        mockCallbackCheckoutParams.put("checkout-reference", "192387192837195");
        mockCallbackCheckoutParams.put("checkout-transaction-id", transactionId);
        mockCallbackCheckoutParams.put("checkout-status", status);
        mockCallbackCheckoutParams.put("checkout-provider", "nordea");

        return mockCallbackCheckoutParams;
    }

    private MerchantDto updateMerchantConfigurationValueByKey(String merchantId, String namespace, String key, String value) {
        String merchantApiUrl = serviceUrls.getMerchantServiceUrl() + "/merchant/get?merchantId=" + merchantId + "&namespace=" + namespace;
        try {
            JSONObject merchantModel = restServiceClient.queryJsonService(restServiceClient.getClient(), merchantApiUrl);
            log.debug("merchantConfigurationValue: " + merchantModel);

            MerchantDto merchantDto = mapper.readValue(merchantModel.toString(), MerchantDto.class);

            if (!ServiceConfigurationKeys.getMerchantKeys().contains(key)) {
                log.debug("Cant update merchant model - invalid merchant key provided: {}", key);
                return null;
            }

            /* Modify config value by provided key */
            MerchantDto modifiedMerchantDto = ConfigurationParseUtil.modifyMerchantConfigurationValueByKey(merchantDto, key, value);
            String body = mapper.writeValueAsString(modifiedMerchantDto);

            /* Update the modified merchant */
            merchantApiUrl = serviceUrls.getMerchantServiceUrl() + "/merchant/upsert";
            JSONObject updatedMerchantModel = restServiceClient.makePostCall(merchantApiUrl, body);
            MerchantDto updatedMerchantDto = mapper.readValue(updatedMerchantModel.toString(), MerchantDto.class);

            return updatedMerchantDto;
        } catch (Exception e) {
            log.debug(e.toString());
            log.debug("Failed to update merchant model with given namespace {}, merchantId {} and key {}", namespace, merchantId, key);
            return null;
        }
    }
}
