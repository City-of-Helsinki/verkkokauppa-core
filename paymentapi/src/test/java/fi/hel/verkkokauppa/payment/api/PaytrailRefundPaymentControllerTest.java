package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundReturnDto;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.model.refund.RefundPaymentStatus;
import fi.hel.verkkokauppa.payment.repository.refund.RefundPaymentRepository;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import fi.hel.verkkokauppa.payment.utils.PaytrailPaymentCreator;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.junit.After;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Before running tests - ensure that following endpoints have been called
 *   so data needed for tests has been initialized:
 * - .../namespace/init/merchant/initialize-test-data
 * - .../merchant/paytrail-secret/add"
 * */
@SpringBootTest
@Slf4j
class PaytrailRefundPaymentControllerTest extends PaytrailPaymentCreator {

    private static final String NAMESPACE = "venepaikat";
    private static final String TEST_PAYTRAIL_MERCHANT_ID = "375917";
    private static final String TEST_PAYTRAIL_SECRET_KEY = "SAIPPUAKAUPPIAS";

    @Autowired
    private PaytrailRefundPaymentController paytrailRefundPaymentController;
    @Autowired
    private RefundPaymentRepository refundPaymentRepository;

    private ArrayList<String> toBeDeletedRefundPaymentIds = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            toBeDeletedRefundPaymentIds.forEach(refundPaymentId -> {
                log.info(refundPaymentId);
                refundPaymentRepository.deleteById(refundPaymentId);
            });
            // Clear list because all deleted
            toBeDeletedRefundPaymentIds = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedRefundPaymentIds = new ArrayList<>();
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    void testCheckRefundReturnUrl() throws ExecutionException, InterruptedException {
        /* Create a refund payment from mock refund to make the whole return url check process possible*/
        PaytrailClient client = new PaytrailClient(merchantId, secretKey);
        String orderId = "dummy-order-id";

        String paymentId = orderId + "_at_" + UUID.randomUUID();
        PaytrailPaymentResponse paymentResponse = createTestNormalMerchantPayment(
                client,
                10,
                paymentId
        );
        // Manual step for testing -> go to href and then use nordea to approve payment
        log.info(paymentResponse.getHref());
        log.info(paymentResponse.getTransactionId());

        String merchantId = getFirstMerchantIdFromNamespace(NAMESPACE);
        RefundRequestDataDto refundRequestDataDto = createRefundRequestDto(paymentResponse.getTransactionId(), merchantId);

        ResponseEntity<RefundPayment> refundPaymentResponse = paytrailRefundPaymentController.createRefundPaymentFromRefund(refundRequestDataDto);
        RefundPayment refundPayment = refundPaymentResponse.getBody();
        Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());

        /* Create callback and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(refundPayment.getRefundId(), refundPayment.getRefundTransactionId(), mockStatus);

        String mockSettlementReference = "8739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams.put("checkout-settlement-reference", mockSettlementReference);

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String signature = PaytrailSignatureService.calculateSignature(filteredParams, null, TEST_PAYTRAIL_SECRET_KEY);
            mockCallbackCheckoutParams.put("signature", signature);

            ResponseEntity<RefundReturnDto> response = paytrailRefundPaymentController.checkRefundReturnUrl(merchantId, signature, mockStatus, refundPayment.getRefundPaymentId(), mockCallbackCheckoutParams);
            RefundReturnDto refundReturnDto = response.getBody();

            Assertions.assertTrue(refundReturnDto.isValid());
            Assertions.assertTrue(refundReturnDto.isRefundPaid());
            Assertions.assertFalse(refundReturnDto.isCanRetry());

            RefundPayment updatedRefundPayment = refundPaymentRepository.findById(refundPayment.getRefundPaymentId()).get();
            Assertions.assertEquals(RefundPaymentStatus.PAID_ONLINE, updatedRefundPayment.getStatus());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        toBeDeletedRefundPaymentIds.add(refundPayment.getRefundPaymentId());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCheckRefundReturnUrlWithInvalidSignature() throws ExecutionException, InterruptedException {
        /* Create a refund payment from mock refund to make the whole return url check process possible*/
        PaytrailClient client = new PaytrailClient(merchantId, secretKey);
        String orderId = "dummy-order-id";

        String paymentId = orderId + "_at_" + UUID.randomUUID();
        PaytrailPaymentResponse paymentResponse = createTestNormalMerchantPayment(
                client,
                10,
                paymentId
        );
        // Manual step for testing -> go to href and then use nordea to approve payment
        log.info(paymentResponse.getHref());
        log.info(paymentResponse.getTransactionId());

        String merchantId = getFirstMerchantIdFromNamespace(NAMESPACE);
        RefundRequestDataDto refundRequestDataDto = createRefundRequestDto(paymentResponse.getTransactionId(), merchantId);

        ResponseEntity<RefundPayment> refundPaymentResponse = paytrailRefundPaymentController.createRefundPaymentFromRefund(refundRequestDataDto);
        RefundPayment refundPayment = refundPaymentResponse.getBody();
        Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());

        /* Create callback and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(refundPayment.getRefundPaymentId(), refundPayment.getRefundTransactionId(), mockStatus);

        String mockSettlementReference = "8739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams.put("checkout-settlement-reference", mockSettlementReference);

        String mockSignature = "invalid-739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams.put("signature", mockSignature);

        ResponseEntity<RefundReturnDto> response = paytrailRefundPaymentController.checkRefundReturnUrl(merchantId, mockSignature, mockStatus, refundPayment.getRefundPaymentId(), mockCallbackCheckoutParams);
        RefundReturnDto refundReturnDto = response.getBody();

        /* Verify correct refund return dto - should be invalid, because signature mismatches */
        Assertions.assertFalse(refundReturnDto.isValid());
        Assertions.assertTrue(refundReturnDto.isRefundPaid());
        Assertions.assertFalse(refundReturnDto.isCanRetry());

        /* Verify that refundPayment status has not changed - CREATED */
        RefundPayment updatedRefundPayment = refundPaymentRepository.findById(refundPayment.getRefundPaymentId()).get();
        Assertions.assertEquals(RefundPaymentStatus.CREATED, updatedRefundPayment.getStatus());

        toBeDeletedRefundPaymentIds.add(refundPayment.getRefundPaymentId());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCheckRefundReturnUrlWithFailStatus() throws ExecutionException, InterruptedException {
        /* Create a refund payment from mock refund to make the whole return url check process possible*/
        PaytrailClient client = new PaytrailClient(merchantId, secretKey);
        String orderId = "dummy-order-id";

        String paymentId = orderId + "_at_" + UUID.randomUUID();
        PaytrailPaymentResponse paymentResponse = createTestNormalMerchantPayment(
                client,
                10,
                paymentId
        );
        // Manual step for testing -> go to href and then use nordea to approve payment
        log.info(paymentResponse.getHref());
        log.info(paymentResponse.getTransactionId());

        String merchantId = getFirstMerchantIdFromNamespace(NAMESPACE);
        RefundRequestDataDto refundRequestDataDto = createRefundRequestDto(paymentResponse.getTransactionId(), merchantId);

        ResponseEntity<RefundPayment> refundPaymentResponse = paytrailRefundPaymentController.createRefundPaymentFromRefund(refundRequestDataDto);
        RefundPayment refundPayment = refundPaymentResponse.getBody();
        Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());

        /* Create callback and check url */
        String mockStatus = "fail";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(refundPayment.getRefundPaymentId(), refundPayment.getRefundTransactionId(), mockStatus);

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, TEST_PAYTRAIL_SECRET_KEY);
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<RefundReturnDto> response = paytrailRefundPaymentController.checkRefundReturnUrl(merchantId, mockSignature, mockStatus, refundPayment.getRefundPaymentId(), mockCallbackCheckoutParams);
            RefundReturnDto refundReturnDto = response.getBody();

            /* Verify correct refund return dto - should be only valid and retryable */
            Assertions.assertTrue(refundReturnDto.isValid());
            Assertions.assertFalse(refundReturnDto.isRefundPaid());
            Assertions.assertTrue(refundReturnDto.isCanRetry());

            /* Verify that refund status is still CREATED */
            RefundPayment updatedRefundPayment = refundPaymentRepository.findById(refundPayment.getRefundPaymentId()).get();
            Assertions.assertEquals(RefundPaymentStatus.CREATED, updatedRefundPayment.getStatus());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        toBeDeletedRefundPaymentIds.add(refundPayment.getRefundPaymentId());
    }

    private Map<String, String> createMockCallbackParams(String refundId, String transactionId, String status) {
        Map<String, String> mockCallbackCheckoutParams = new HashMap<>();
        mockCallbackCheckoutParams.put("checkout-account", TEST_PAYTRAIL_MERCHANT_ID);
        mockCallbackCheckoutParams.put("checkout-algorithm", "sha256");
        mockCallbackCheckoutParams.put("checkout-amount", "2964");
        mockCallbackCheckoutParams.put("checkout-stamp", refundId);
        mockCallbackCheckoutParams.put("checkout-reference", "192387192837195");
        mockCallbackCheckoutParams.put("checkout-transaction-id", transactionId);
        mockCallbackCheckoutParams.put("checkout-status", status);
        mockCallbackCheckoutParams.put("checkout-provider", "nordea");

        return mockCallbackCheckoutParams;
    }

    private RefundRequestDataDto createRefundRequestDto(String transactionId, String merchantId) {
        RefundRequestDataDto refundRequestDataDto= new RefundRequestDataDto();

        OrderWrapper orderWrapper = new OrderWrapper();
        OrderDto order = new OrderDto();
        order.setType(OrderType.ORDER);
        orderWrapper.setOrder(order);
        refundRequestDataDto.setOrder(orderWrapper);

        RefundAggregateDto refundAggregateDto = new RefundAggregateDto();
        RefundDto refundDto = new RefundDto();
        String refundId = "refund-id";
        refundDto.setRefundId(refundId);
        refundDto.setStatus("confirmed");
        String user = "dummy_user";
        refundDto.setUser(user);
        refundDto.setNamespace(NAMESPACE);
        refundDto.setOrderId(orderWrapper.getOrder().getOrderId());
        refundDto.setPriceNet("10");
        refundDto.setPriceVat("0");
        refundDto.setPriceTotal("10");
        refundDto.setCustomerEmail(UUID.randomUUID() + "@ambientia.fi");
        refundAggregateDto.setRefund(refundDto);

        ArrayList<RefundItemDto> refundItemDtos = new ArrayList<>();
        RefundItemDto itemDto = new RefundItemDto();
        itemDto.setMerchantId(merchantId);
        refundItemDtos.add(itemDto);
        refundAggregateDto.setItems(refundItemDtos);
        refundRequestDataDto.setRefund(refundAggregateDto);

        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setShopInShopPayment(false);
        paymentDto.setPaytrailTransactionId(transactionId);
        String paymentMethod = "payment-method";
        paymentDto.setPaymentMethod(paymentMethod);
        refundRequestDataDto.setPayment(paymentDto);

        return refundRequestDataDto;
    }

}