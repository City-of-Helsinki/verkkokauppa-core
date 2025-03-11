package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundReturnDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.model.refund.RefundPaymentStatus;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.repository.refund.RefundPaymentRepository;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import fi.hel.verkkokauppa.payment.testing.data.CreateRefundAccountingRequestDto;
import fi.hel.verkkokauppa.payment.testing.data.OrderAggregateDto;
import fi.hel.verkkokauppa.payment.testing.data.ProductAccountingDto;
import fi.hel.verkkokauppa.payment.testing.model.Refund;
import fi.hel.verkkokauppa.payment.testing.model.RefundItem;
import fi.hel.verkkokauppa.payment.utils.PaytrailPaymentCreator;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.json.JSONObject;
import org.junit.After;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Before running tests - ensure that following endpoints have been called
 * so data needed for tests has been initialized:
 * - .../namespace/init/merchant/initialize-test-data
 * - .../merchant/paytrail-secret/add"
 */
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
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ServiceUrls serviceUrls;
    @Autowired
    private RestServiceClient restServiceClient;
    @Autowired
    private PaytrailPaymentContextBuilder paymentContextBuilder;

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
    void testCheckRefundReturnUrl() throws Exception {
        /* Create a refund payment from order to make the whole return url check process possible*/
        String firstMerchantId = getFirstMerchantIdFromNamespace(NAMESPACE);
        PaytrailPaymentContext context = paymentContextBuilder.buildFor(NAMESPACE, firstMerchantId, false);

        PaytrailClient client = new PaytrailClient(context.getPaytrailMerchantId(), context.getPaytrailSecretKey());

        OrderAggregateDto testOrder = createTestOrderWithItems(firstMerchantId);
        Payment payment = createTestPayment(testOrder);

        PaytrailPaymentResponse paymentResponse = createTestNormalMerchantPayment(
                client,
                Integer.parseInt(testOrder.getOrder().getPriceTotal()),
                payment.getPaymentId()
        );
        // Manual step for testing -> go to href and then use nordea to approve payment
        log.info(paymentResponse.getHref());
        log.info(paymentResponse.getTransactionId());

        //
        // Breakpoint here!!!
        //
        RefundRequestDataDto refundRequestDataDto = createRefundRequestDto(paymentResponse.getTransactionId(), firstMerchantId);

        ResponseEntity<RefundPayment> refundPaymentResponse = paytrailRefundPaymentController.createRefundPaymentFromRefund(refundRequestDataDto);
        RefundPayment refundPayment = refundPaymentResponse.getBody();
        Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());

        /* Create callback and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(refundPayment.getRefundId(), refundPayment.getRefundTransactionId(), mockStatus, context.getPaytrailMerchantId());

        String mockSettlementReference = "8739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams.put("checkout-settlement-reference", mockSettlementReference);

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String signature = PaytrailSignatureService.calculateSignature(filteredParams, null, context.getPaytrailSecretKey());
            mockCallbackCheckoutParams.put("signature", signature);

            ResponseEntity<RefundReturnDto> response = paytrailRefundPaymentController.checkRefundReturnUrl(firstMerchantId, signature, mockStatus, refundPayment.getRefundPaymentId(), mockCallbackCheckoutParams);
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
    public void testCheckRefundReturnUrlWithInvalidSignature() throws Exception {
        /* Create a refund payment from order to make the whole return url check process possible*/
        String firstMerchantId = getFirstMerchantIdFromNamespace(NAMESPACE);
        PaytrailPaymentContext context = paymentContextBuilder.buildFor(NAMESPACE, firstMerchantId, false);

        PaytrailClient client = new PaytrailClient(context.getPaytrailMerchantId(), context.getPaytrailSecretKey());

        OrderAggregateDto testOrder = createTestOrderWithItems(firstMerchantId);
        Payment payment = createTestPayment(testOrder);

        PaytrailPaymentResponse paymentResponse = createTestNormalMerchantPayment(
                client,
                Integer.parseInt(testOrder.getOrder().getPriceTotal()),
                payment.getPaymentId()
        );

        // Manual step for testing -> go to href and then use nordea to approve payment
        log.info(paymentResponse.getHref());
        log.info(paymentResponse.getTransactionId());

        //
        // Breakpoint here!!!
        //
        RefundRequestDataDto refundRequestDataDto = createRefundRequestDto(paymentResponse.getTransactionId(), firstMerchantId);

        ResponseEntity<RefundPayment> refundPaymentResponse = paytrailRefundPaymentController.createRefundPaymentFromRefund(refundRequestDataDto);
        RefundPayment refundPayment = refundPaymentResponse.getBody();
        Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());

        /* Create callback and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(refundPayment.getRefundPaymentId(), refundPayment.getRefundTransactionId(), mockStatus, context.getPaytrailMerchantId());

        String mockSettlementReference = "8739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams.put("checkout-settlement-reference", mockSettlementReference);

        String mockSignature = "invalid-739a8a8-1ce0-4729-89ce-40065fd424a2";
        mockCallbackCheckoutParams.put("signature", mockSignature);

        ResponseEntity<RefundReturnDto> response = paytrailRefundPaymentController.checkRefundReturnUrl(firstMerchantId, mockSignature, mockStatus, refundPayment.getRefundPaymentId(), mockCallbackCheckoutParams);
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
    public void testCheckRefundReturnUrlWithFailStatus() throws Exception {
        /* Create a refund payment from order to make the whole return url check process possible*/
        String firstMerchantId = getFirstMerchantIdFromNamespace(NAMESPACE);
        PaytrailPaymentContext context = paymentContextBuilder.buildFor(NAMESPACE, firstMerchantId, false);

        PaytrailClient client = new PaytrailClient(context.getPaytrailMerchantId(), context.getPaytrailSecretKey());

        OrderAggregateDto testOrder = createTestOrderWithItems(firstMerchantId);
        Payment payment = createTestPayment(testOrder);

        PaytrailPaymentResponse paymentResponse = createTestNormalMerchantPayment(
                client,
                Integer.parseInt(testOrder.getOrder().getPriceTotal()),
                payment.getPaymentId()
        );
        // Manual step for testing -> go to href and then use nordea to approve payment
        log.info(paymentResponse.getHref());
        log.info(paymentResponse.getTransactionId());

        //
        // Breakpoint here!!!
        //
        RefundRequestDataDto refundRequestDataDto = createRefundRequestDto(paymentResponse.getTransactionId(), firstMerchantId);

        ResponseEntity<RefundPayment> refundPaymentResponse = paytrailRefundPaymentController.createRefundPaymentFromRefund(refundRequestDataDto);
        RefundPayment refundPayment = refundPaymentResponse.getBody();
        Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());

        /* Create callback and check url */
        String mockStatus = "fail";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(refundPayment.getRefundPaymentId(), refundPayment.getRefundTransactionId(), mockStatus, context.getPaytrailMerchantId());

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, context.getPaytrailSecretKey());
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<RefundReturnDto> response = paytrailRefundPaymentController.checkRefundReturnUrl(firstMerchantId, mockSignature, mockStatus, refundPayment.getRefundPaymentId(), mockCallbackCheckoutParams);
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

    @Test
    @RunIfProfile(profile = "local")
    public void testRefundWithProperRefund() throws Exception {
        /* Create a refund payment from order to make the whole return url check process possible*/
        String firstMerchantId = getFirstMerchantIdFromNamespace(NAMESPACE);
        PaytrailPaymentContext context = paymentContextBuilder.buildFor(NAMESPACE, firstMerchantId, false);

        PaytrailClient client = new PaytrailClient(context.getPaytrailMerchantId(), context.getPaytrailSecretKey());

        OrderAggregateDto testOrder = createTestOrderWithItems(firstMerchantId);
        Payment payment = createTestPayment(testOrder);

        PaytrailPaymentResponse paymentResponse = createTestNormalMerchantPayment(
                client,
                Integer.parseInt(testOrder.getOrder().getPriceTotal()),
                payment.getPaymentId()
        );
        // Manual step for testing -> go to href and then use nordea to approve payment
        log.info(paymentResponse.getHref());
        log.info(paymentResponse.getTransactionId());

        //
        // Breakpoint here!!!
        //
        RefundAggregateDto refundAggregateDto = createPartialRefund(firstMerchantId, testOrder.getOrder().getOrderId());

        //
        RefundDto renfundDto = confirmRefund(refundAggregateDto.getRefund().getRefundId());
        refundAggregateDto.setRefund(renfundDto);
        RefundRequestDataDto refundRequestDataDto = createRefundRequestDto(paymentResponse.getTransactionId(), testOrder, refundAggregateDto);
        ResponseEntity<RefundPayment> refundPaymentResponse = paytrailRefundPaymentController.createRefundPaymentFromRefund(refundRequestDataDto);
        RefundPayment refundPayment = refundPaymentResponse.getBody();

        Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());

        /* Create callback and check url */
        String mockStatus = "fail";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(refundPayment.getRefundPaymentId(), refundPayment.getRefundTransactionId(), mockStatus, context.getPaytrailMerchantId());

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, context.getPaytrailSecretKey());
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<RefundReturnDto> response = paytrailRefundPaymentController.checkRefundReturnUrl(firstMerchantId, mockSignature, mockStatus, refundPayment.getRefundPaymentId(), mockCallbackCheckoutParams);
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
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testMultipleRefundsWithOneOrder() throws Exception {
        /* Create a refund payment from order to make the whole return url check process possible*/
        String firstMerchantId = getFirstMerchantIdFromNamespace(NAMESPACE);
        PaytrailPaymentContext context = paymentContextBuilder.buildFor(NAMESPACE, firstMerchantId, false);

        PaytrailClient client = new PaytrailClient(context.getPaytrailMerchantId(), context.getPaytrailSecretKey());

        OrderAggregateDto testOrder = createTestOrderWithItems(firstMerchantId);
        Payment payment = createTestPayment(testOrder);

        PaytrailPaymentResponse paymentResponse = createTestNormalMerchantPayment(
                client,
                Integer.parseInt(testOrder.getOrder().getPriceTotal()),
                payment.getPaymentId()
        );
        // Manual step for testing -> go to href and then use nordea to approve payment
        log.info(paymentResponse.getHref());
        log.info(paymentResponse.getTransactionId());

        //
        // Breakpoint here!!!
        //
        RefundAggregateDto refundAggregateDto = createPartialRefund(firstMerchantId, testOrder.getOrder().getOrderId());

        RefundDto renfundDto = confirmRefund(refundAggregateDto.getRefund().getRefundId());
        refundAggregateDto.setRefund(renfundDto);
        RefundRequestDataDto refundRequestDataDto = createRefundRequestDto(paymentResponse.getTransactionId(), testOrder, refundAggregateDto);
        ResponseEntity<RefundPayment> refundPaymentResponse = paytrailRefundPaymentController.createRefundPaymentFromRefund(refundRequestDataDto);
        RefundPayment refundPayment = refundPaymentResponse.getBody();

        Assertions.assertEquals(RefundPaymentStatus.CREATED, refundPayment.getStatus());

        /* Create callback and check url */
        String mockStatus = "ok";
        Map<String, String> mockCallbackCheckoutParams = createMockCallbackParams(refundPayment.getRefundPaymentId(), refundPayment.getRefundTransactionId(), mockStatus, context.getPaytrailMerchantId());

        TreeMap<String, String> filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, context.getPaytrailSecretKey());
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<RefundReturnDto> response = paytrailRefundPaymentController.checkRefundReturnUrl(firstMerchantId, mockSignature, mockStatus, refundPayment.getRefundPaymentId(), mockCallbackCheckoutParams);
            RefundReturnDto refundReturnDto = response.getBody();

            /* Verify correct refund return dto - should be only valid and retryable */
            Assertions.assertTrue(refundReturnDto.isValid());
            Assertions.assertTrue(refundReturnDto.isRefundPaid());
            Assertions.assertFalse(refundReturnDto.isCanRetry());

            /* Verify that refund status is still CREATED */
            RefundPayment updatedRefundPayment = refundPaymentRepository.findById(refundPayment.getRefundPaymentId()).get();
            Assertions.assertEquals(RefundPaymentStatus.PAID_ONLINE, updatedRefundPayment.getStatus());


        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }

        // 2nd refund
        RefundAggregateDto refund2AggregateDto = createPartialRefund(firstMerchantId, testOrder.getOrder().getOrderId());

        RefundDto renfund2Dto = confirmRefund(refund2AggregateDto.getRefund().getRefundId());
        refundAggregateDto.setRefund(renfund2Dto);
        RefundRequestDataDto refund2RequestDataDto = createRefundRequestDto(paymentResponse.getTransactionId(), testOrder, refundAggregateDto);
        ResponseEntity<RefundPayment> refund2PaymentResponse = paytrailRefundPaymentController.createRefundPaymentFromRefund(refund2RequestDataDto);
        RefundPayment refund2Payment = refund2PaymentResponse.getBody();

        Assertions.assertEquals(RefundPaymentStatus.CREATED, refund2Payment.getStatus());

        filteredParams = PaytrailSignatureService.filterCheckoutQueryParametersMap(mockCallbackCheckoutParams);
        try {
            String mockSignature = PaytrailSignatureService.calculateSignature(filteredParams, null, context.getPaytrailSecretKey());
            mockCallbackCheckoutParams.put("signature", mockSignature);

            ResponseEntity<RefundReturnDto> response = paytrailRefundPaymentController.checkRefundReturnUrl(firstMerchantId, mockSignature, mockStatus, refund2Payment.getRefundPaymentId(), mockCallbackCheckoutParams);
            RefundReturnDto refund2ReturnDto = response.getBody();

            /* Verify correct refund return dto - should be only valid and retryable */
            Assertions.assertTrue(refund2ReturnDto.isValid());
            Assertions.assertTrue(refund2ReturnDto.isRefundPaid());
            Assertions.assertFalse(refund2ReturnDto.isCanRetry());

            /* Verify that refund status is still CREATED */
            RefundPayment updatedRefund2Payment = refundPaymentRepository.findById(refund2Payment.getRefundPaymentId()).get();
            Assertions.assertEquals(RefundPaymentStatus.PAID_ONLINE, updatedRefund2Payment.getStatus());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> createMockCallbackParams(String refundId, String transactionId, String status, String testMerchantId) {
        Map<String, String> mockCallbackCheckoutParams = new HashMap<>();
        mockCallbackCheckoutParams.put("checkout-account", testMerchantId);
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
        RefundRequestDataDto refundRequestDataDto = new RefundRequestDataDto();

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
        refundDto.setCustomerEmail(UUID.randomUUID() + "@hiq.fi");
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

    private RefundRequestDataDto createRefundRequestDto(String transactionId, OrderAggregateDto order, RefundAggregateDto refundAggregateDto) {
        RefundRequestDataDto refundRequestDataDto = new RefundRequestDataDto();

        OrderDto orderDto = new OrderDto();
        orderDto.setOrderId(order.getOrder().getOrderId());
        orderDto.setType(order.getOrder().getType());

        OrderWrapper orderWrapper = new OrderWrapper();
        orderWrapper.setOrder(orderDto);
        refundRequestDataDto.setOrder(orderWrapper);

        refundRequestDataDto.setRefund(refundAggregateDto);

        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setShopInShopPayment(false);
        paymentDto.setPaytrailTransactionId(transactionId);
        String paymentMethod = "payment-method";
        paymentDto.setPaymentMethod(paymentMethod);
        refundRequestDataDto.setPayment(paymentDto);

        // "/refund/paytrail/createFromRefund"

        return refundRequestDataDto;
    }

    RefundDto confirmRefund(String refundId) throws Exception {

    String refundApiUrl = serviceUrls.getOrderServiceUrl() + "/refund-admin/confirm";

    JSONObject refundConfirmResponse = restServiceClient.makePostCall(refundApiUrl+"?refundId=" + refundId, "refundId="+refundId);

    RefundDto refundDto = mapper.readValue(refundConfirmResponse.toString(), RefundDto.class);

    return refundDto;
}

    String createTestOrder() throws Exception {
        // create dummy order
        String orderApiUrl = serviceUrls.getOrderServiceUrl() + "/order/create";
        JSONObject orderResponse = restServiceClient.makeGetCall(orderApiUrl + "?namespace=" + NAMESPACE + "&user=dummyUser");

        String orderId = orderResponse.getJSONObject("order").getString("orderId");
        return orderId;
    }

    OrderAggregateDto createTestOrderWithItems(String merchantId) throws Exception {
        // create dummy order
        String orderApiUrl = serviceUrls.getOrderServiceUrl() + "/order/createWithItems";

        List<OrderItemDto> items = new ArrayList<>();

        OrderItemDto itemDto = new OrderItemDto();
//        itemDto.setOrderId(orderId);
        itemDto.setOrderItemId(UUID.randomUUID().toString());
        itemDto.setProductId("b86337e8-68a0-3599-a18b-754ffae53f5a");
        itemDto.setMerchantId(merchantId);
        itemDto.setUnit("pcs");
        itemDto.setPriceGross(BigDecimal.valueOf(10));
        itemDto.setRowPriceTotal(BigDecimal.valueOf(20));
        itemDto.setPriceNet(BigDecimal.valueOf(9));
        itemDto.setRowPriceNet(BigDecimal.valueOf(18));
        itemDto.setPriceVat(BigDecimal.valueOf(1));
        itemDto.setRowPriceVat(BigDecimal.valueOf(2));
        itemDto.setQuantity(2);
        itemDto.setProductName("Product Name");
        itemDto.setProductLabel("Product Label");
        itemDto.setProductDescription("Product Description");


        items.add(itemDto);

        OrderAggregateDto requestOrderDto = new OrderAggregateDto(
                new OrderDto(
                        null,
                        NAMESPACE,
                        "dummyUser",
                        DateTimeUtil.getFormattedDateTime().minusDays(1).toString(),
                        null,
                        "order",
                        "firstName",
                        "lastNAme",
                        "test@hiq.fi",
                        "18",
                        "2",
                        "20",
                        null,

                        null
                     ),
                items,
                null,
                null);

        String body = mapper.writeValueAsString(requestOrderDto);
        JSONObject orderResponse = restServiceClient.makePostCall(orderApiUrl, body);

        String newOrderId = orderResponse.getJSONObject("order").getString("orderId");
        requestOrderDto.getOrder().setOrderId(newOrderId);
        log.info("Created order id: " + newOrderId);
        return requestOrderDto;
    }

    RefundAggregateDto createPartialRefund(String merchantId, String orderId) throws Exception {
        // create dummy order
        String refundApiUrl = serviceUrls.getOrderServiceUrl() + "/refund/create";

        List<RefundItemDto> items = new ArrayList<>();

        RefundItemDto itemDto = new RefundItemDto();
//        itemDto.setOrderId(orderId);
        itemDto.setOrderItemId(UUID.randomUUID().toString());
        itemDto.setProductId("30a245ed-5fca-4fcf-8b2a-cdf1ce6fca0d");
        itemDto.setMerchantId(merchantId);
        itemDto.setUnit("pcs");
        itemDto.setPriceGross("10");
        itemDto.setRowPriceTotal("10");
        itemDto.setPriceNet("9");
        itemDto.setRowPriceNet("9");
        itemDto.setPriceVat("1");
        itemDto.setRowPriceVat("1");
        itemDto.setQuantity(1);
        itemDto.setProductName("Product Name");
        itemDto.setProductLabel("Product Label");
        itemDto.setProductDescription("Product Description");


        items.add(itemDto);

        RefundAggregateDto requestRefundDto = new RefundAggregateDto(
                new RefundDto(
                        null,
                        orderId,
                        NAMESPACE,
                        "dummyUser",
                        DateTimeUtil.getFormattedDateTime().minusDays(1).toString(),
                        null,
                        "firstName",
                        "lastNAme",
                        "test@hiq.fi",
                        null,
                        "refund reason",
                        "9",
                        "1",
                        "10",
                        null),
                items);

        String body = mapper.writeValueAsString(requestRefundDto);
        JSONObject refundResponse = restServiceClient.makePostCall(refundApiUrl, body);

        String newRefundId = refundResponse.getJSONObject("refund").getString("refundId");
        requestRefundDto.getRefund().setRefundId(newRefundId);
        log.info("Created refund id: " + newRefundId);
        return requestRefundDto;
    }

    Payment createTestPayment(OrderAggregateDto orderAggregate) {

        OrderDto order = orderAggregate.getOrder();
        String paymentId = order.getOrderId() + "_at_" + UUID.randomUUID();

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setNamespace(order.getNamespace());
        payment.setOrderId(order.getOrderId());
        payment.setUserId(order.getUser());
//        payment.setPaymentMethod(dto.getPaymentMethod());
//        payment.setPaymentMethodLabel(dto.getPaymentMethodLabel());
        payment.setTimestamp(sdf.format(timestamp));
//        payment.setAdditionalInfo("{\"payment_method\": " + dto.getPaymentMethod() + "}");
        payment.setPaymentType("order");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setTotalExclTax(new BigDecimal(order.getPriceNet()));
        payment.setTaxAmount(new BigDecimal(order.getPriceVat()));
        payment.setTotal(new BigDecimal(order.getPriceTotal()));
//        payment.setPaytrailTransactionId("2e60d1e0-0f5a-4f1b-ad95-d9b0fc00202a");
        payment.setShopInShopPayment(false);

        paymentRepository.save(payment);

        return payment;
    }


    public CreateRefundAccountingRequestDto createRefundAccountingRequest(Refund refund, RefundItem item) {
        CreateRefundAccountingRequestDto requestDto = new CreateRefundAccountingRequestDto();
        requestDto.setRefundId(refund.getRefundId());
        requestDto.setOrderId(refund.getOrderId());

        List<ProductAccountingDto> dtos = new ArrayList<>();

        ProductAccountingDto dto = new ProductAccountingDto();
        dto.setProductId(item.getProductId());
        dto.setProject("project");
        dto.setMainLedgerAccount("MainLedgerAccount");
        dto.setCompanyCode("1234");
        dto.setOperationArea("operationArea");
        dto.setInternalOrder("1234");
        dto.setBalanceProfitCenter("balanceProfitCenter");
        dto.setProfitCenter("profitCenter");
        dto.setVatCode("10");

        dtos.add(dto);

        requestDto.setDtos(dtos);

        return requestDto;
    }
}