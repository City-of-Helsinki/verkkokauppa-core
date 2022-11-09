package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class PaytrailPaymentControllerTest extends BaseFunctionalTest {

    @Autowired
    private PaytrailPaymentController paytrailPaymentController;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PayerRepository payerRepository;

    @Autowired
    private PaymentItemRepository paymentItemRepository;

    @Value("${paytrail.aggregate.merchant.id}")
    private String aggregateMerchantId;

    @Value("${paytrail.merchant.secret}")
    private String secretKey;

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
        OrderWrapper orderWrapper = createDummyOrderWrapper();
        OrderItemDto dummyOrderItem = orderWrapper.getItems().get(0);
        paymentRequestDataDto.setOrder(orderWrapper);

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
        Assertions.assertEquals(CommonApiException.class, exception.getClass());
        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        Assertions.assertEquals("rejected-creating-paytrail-payment-for-order-without-user", exception.getErrors().getErrors().get(0).getCode());
        Assertions.assertEquals("rejected creating paytrail payment for order without user, order id [" + orderWrapper.getOrder().getOrderId() + "]", exception.getErrors().getErrors().get(0).getMessage());
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
        Assertions.assertEquals(CommonApiException.class, exception1.getClass());
        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception1.getStatus());
        Assertions.assertEquals("rejected-creating-paytrail-payment-for-unconfirmed-order", exception1.getErrors().getErrors().get(0).getCode());
        Assertions.assertEquals("rejected creating paytrail payment for unconfirmed order, order id [" + orderWrapper1.getOrder().getOrderId() + "]", exception1.getErrors().getErrors().get(0).getMessage());

        /* Test with OrderStatus.CANCELLED */
        OrderWrapper orderWrapper2 = createDummyOrderWrapper();
        orderWrapper2.getOrder().setStatus("cancelled");
        paymentRequestDataDto.setOrder(orderWrapper2);

        CommonApiException exception2 = assertThrows(CommonApiException.class, () -> {
            paytrailPaymentController.createPaymentFromOrder(paymentRequestDataDto);
        });
        Assertions.assertEquals(CommonApiException.class, exception2.getClass());
        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception2.getStatus());
        Assertions.assertEquals("rejected-creating-paytrail-payment-for-unconfirmed-order", exception2.getErrors().getErrors().get(0).getCode());
        Assertions.assertEquals("rejected creating paytrail payment for unconfirmed order, order id [" + orderWrapper2.getOrder().getOrderId() + "]", exception2.getErrors().getErrors().get(0).getMessage());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCheckReturnUrl() {
        /* First create payment from mock order to make the whole return url check process possible*/
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();
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
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, secretKey);
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<PaymentReturnDto> response = paytrailPaymentController.checkReturnUrl(mockSignature, mockStatus, payment.getPaymentId(), mockSettlementReference, mockCallbackCheckoutParams);
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

        ResponseEntity<PaymentReturnDto> response = paytrailPaymentController.checkReturnUrl(mockSignature, mockStatus, payment.getPaymentId(), mockSettlementReference, mockCallbackCheckoutParams);
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
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, secretKey);
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<PaymentReturnDto> response = paytrailPaymentController.checkReturnUrl(mockSignature, mockStatus, payment.getPaymentId(), null, mockCallbackCheckoutParams);
            PaymentReturnDto paymentReturnDto = response.getBody();

            /* Verify correct payment return dto - should be authorized but not paid */
            assertTrue(paymentReturnDto.isValid());
            assertFalse(paymentReturnDto.isPaymentPaid());
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
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, secretKey);
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<PaymentReturnDto> response = paytrailPaymentController.checkReturnUrl(mockSignature, mockStatus, payment.getPaymentId(), null, mockCallbackCheckoutParams);
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
        mockCallbackCheckoutParams.put("checkout-account", aggregateMerchantId);
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
