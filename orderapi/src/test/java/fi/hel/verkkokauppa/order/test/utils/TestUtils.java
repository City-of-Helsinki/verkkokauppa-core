package fi.hel.verkkokauppa.order.test.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.elastic.ElasticSearchRestClientResolver;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.OrderController;
import fi.hel.verkkokauppa.order.api.RefundController;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformerUtils;
import fi.hel.verkkokauppa.order.api.data.transformer.RefundTransformer;
import fi.hel.verkkokauppa.order.logic.subscription.NextDateCalculator;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.subscription.SubscriptionService;
import fi.hel.verkkokauppa.order.test.utils.payment.PaytrailHelper;
import fi.hel.verkkokauppa.order.test.utils.payment.TestPayment;
import fi.hel.verkkokauppa.order.test.utils.payment.TestPaytrailPaymentResponse;
import fi.hel.verkkokauppa.order.test.utils.payment.TestRefundPayment;
import fi.hel.verkkokauppa.order.test.utils.productaccounting.TestProductAccounting;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;

import static fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID;

@Component
@Slf4j
public class TestUtils extends DummyData {

    @Autowired
    private OrderTransformerUtils orderTransformerUtils;

    @Autowired
    private RefundTransformer refundTransformer;

    @Autowired
    private OrderController orderController;

    @Autowired
    private RefundController refundController;

    @Autowired
    private SubscriptionController subscriptionController;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NextDateCalculator nextDateCalculator;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ServiceUrls serviceUrls;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    ElasticSearchRestClientResolver clientResolver;

    /**
     * Exclude field names by providing the field name as a string.
     * For example, if data object has a member variable "firstName" and you want to exclude it -
     * pass it inside a list as a string: Arrays.asList("firstName")
     *
     * @param entity
     * @param exclusions
     * @return
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static boolean hasNullFields(Object entity, List<String> exclusions) throws IllegalAccessException, IllegalArgumentException {
        for (Field f : entity.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            final Object value = f.get(entity);
            final String name = f.getName();

            boolean excludeNext = false;

            for (String exclusion : exclusions) {

                if (exclusion.equals(name)) {
                    excludeNext = true;
                    break;
                }
            }

            if (!excludeNext) {
                if (value == null) {
                    return true;
                }
            }
        }
        return false;
    }

    public ResponseEntity<OrderAggregateDto> generateSubscriptionOrderData(int itemCount, long periodFrequency, String periodUnit, int periodCount) {
        return generateSubscriptionOrderData(itemCount, periodFrequency, periodUnit, periodCount, true, "productId");
    }
    public ResponseEntity<OrderAggregateDto> generateSubscriptionOrderData(int itemCount, long periodFrequency, String periodUnit, int periodCount, boolean includeMetas) {
        return generateSubscriptionOrderData(itemCount, periodFrequency, periodUnit, periodCount, includeMetas, "productId");
    }

    public ResponseEntity<OrderAggregateDto> generateSubscriptionOrderData(int itemCount, long periodFrequency, String periodUnit, int periodCount, boolean includeMetas, String productId) {
        Order order = generateDummyOrder();

        order.setEndDate(LocalDateTime.now().plusMonths(1));

        order.setNamespace("venepaikat");
        order.setCustomerEmail(UUID.randomUUID().toString() + "@hiq.fi");
        List<OrderItem> orderItems = generateDummyOrderItemList(order, itemCount);
        orderItems.get(0).setPeriodFrequency(periodFrequency);
        orderItems.get(0).setPeriodUnit(periodUnit);
        orderItems.get(0).setPeriodCount(periodCount);
        orderItems.get(0).setBillingStartDate(LocalDateTime.now());
        orderItems.get(0).setStartDate(LocalDateTime.now());
        orderItems.get(0).setPriceGross("100");
        orderItems.get(0).setPriceNet("98.5");
        orderItems.get(0).setPriceVat("1.5");
        orderItems.get(0).setVatPercentage("1.5");
        orderItems.get(0).setProductId(productId);
        orderItems.get(0).setMerchantId(getFirstMerchantIdFromNamespace("venepaikat"));
        List<OrderItemMeta> orderItemMetas;
        if( includeMetas == true ) {
            orderItemMetas = generateDummyOrderItemMetaList(orderItems);
        }
        else
        {
            orderItemMetas = new ArrayList<>();
        }

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order, orderItems, orderItemMetas);

        return orderController.createWithItems(orderAggregateDto);
    }

    public ResponseEntity<OrderAggregateDto> createNewOrderToDatabase(int itemCount) {
        Order order = generateDummyOrder();

        order.setNamespace("venepaikat");
        order.setCustomerEmail(UUID.randomUUID().toString() + "@hiq.fi");
        List<OrderItem> orderItems = generateDummyOrderItemList(order, itemCount);
        orderItems.forEach(orderItem -> orderItem.setPriceGross("124"));
        orderItems.forEach(orderItem -> orderItem.setMerchantId("124"));
        List<OrderItemMeta> orderItemMetas = generateDummyOrderItemMetaList(orderItems);

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order, orderItems, orderItemMetas);

        return orderController.createWithItems(orderAggregateDto);
    }

    public ResponseEntity<RefundAggregateDto> createNewRefundToDatabase(int itemCount, String orderId) {
        Refund refund = generateDummyRefund(orderId);

        refund.setNamespace("venepaikat");
        refund.setCustomerEmail(UUID.randomUUID().toString() + "@hiq.fi");
        List<RefundItem> refundItems = generateDummyRefundItemList(refund, orderId, itemCount);
        refundItems.forEach(refundItem -> refundItem.setPriceGross("124"));
        refundItems.forEach(refundItem -> refundItem.setMerchantId("124"));

        RefundAggregateDto refundAggregateDto = refundTransformer.transformToDto(refund, refundItems);

        return refundController.createRefund(refundAggregateDto);
    }

    public ResponseEntity<OrderAggregateDto> createNewOrderToDatabase(int itemCount, String merchantId) {
        Order order = generateDummyOrder();

        order.setNamespace("venepaikat");
        order.setCustomerEmail(UUID.randomUUID().toString() + "@hiq.fi");
        List<OrderItem> orderItems = generateDummyOrderItemList(order, itemCount);
        orderItems.forEach(orderItem -> orderItem.setPriceGross("100"));
        orderItems.forEach(orderItem -> orderItem.setPriceVat("100"));
        orderItems.forEach(orderItem -> orderItem.setPriceNet("100"));
        orderItems.forEach(orderItem -> orderItem.setRowPriceNet("100"));
        orderItems.forEach(orderItem -> orderItem.setRowPriceVat("100"));
        orderItems.forEach(orderItem -> orderItem.setRowPriceTotal("100"));
        orderItems.forEach(orderItem -> orderItem.setMerchantId(merchantId));
        List<OrderItemMeta> orderItemMetas = generateDummyOrderItemMetaList(orderItems);

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order, orderItems, orderItemMetas);

        return orderController.createWithItems(orderAggregateDto);
    }

    public ResponseEntity<OrderAggregateDto> createNewOrderWithFreeItemToDatabase(int itemCount, String merchantId) {
        Order order = generateDummyOrder();

        order.setNamespace("venepaikat");
        order.setCustomerEmail(UUID.randomUUID().toString() + "@hiq.fi");
        List<OrderItem> orderItems = generateDummyOrderItemList(order, itemCount);
        orderItems.forEach(orderItem -> orderItem.setPriceGross("0.0"));
        orderItems.forEach(orderItem -> orderItem.setPriceVat("0.0"));
        orderItems.forEach(orderItem -> orderItem.setPriceNet("0.0"));
        orderItems.forEach(orderItem -> orderItem.setRowPriceNet("0.0"));
        orderItems.forEach(orderItem -> orderItem.setRowPriceVat("0.0"));
        orderItems.forEach(orderItem -> orderItem.setRowPriceTotal("0.0"));
        orderItems.add(generateDummyFreeOrderItem(order));
        orderItems.forEach(orderItem -> orderItem.setMerchantId(merchantId));
        List<OrderItemMeta> orderItemMetas = generateDummyOrderItemMetaList(orderItems);

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order, orderItems, orderItemMetas);

        return orderController.createWithItems(orderAggregateDto);
    }

    public ResponseEntity<OrderAggregateDto> createNewFreeOrderToDatabase(int itemCount, String merchantId) {
        Order order = generateDummyOrder();

        order.setNamespace("venepaikat");
        order.setCustomerEmail(UUID.randomUUID().toString() + "@hiq.fi");
        List<OrderItem> orderItems = generateDummyOrderItemList(order, itemCount);
        orderItems.forEach(orderItem -> orderItem.setPriceGross("0"));
        orderItems.forEach(orderItem -> orderItem.setPriceVat("0"));
        orderItems.forEach(orderItem -> orderItem.setPriceNet("0"));
        orderItems.forEach(orderItem -> orderItem.setRowPriceNet("0"));
        orderItems.forEach(orderItem -> orderItem.setRowPriceVat("0"));
        orderItems.forEach(orderItem -> orderItem.setRowPriceTotal("0"));
        orderItems.forEach(orderItem -> orderItem.setMerchantId(merchantId));
        List<OrderItemMeta> orderItemMetas = generateDummyOrderItemMetaList(orderItems);

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order, orderItems, orderItemMetas);

        return orderController.createWithItems(orderAggregateDto);
    }


    public ResponseEntity<SubscriptionIdsDto> createSubscriptions(ResponseEntity<OrderAggregateDto> response) {
        return subscriptionController.createSubscriptionsFromOrder(response.getBody());
    }

    public Subscription createAndGetMonthlySubscription() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.MONTHLY, 1);
        String order1Id = orderResponse.getBody().getOrder().getOrderId();
        Order order1 = orderService.findById(order1Id);

        Assertions.assertEquals(OrderType.SUBSCRIPTION, order1.getType());

        // Get orderItems
        List<OrderItem> orderItems = orderItemService.findByOrderId(order1Id);

        Assertions.assertEquals(1, orderItems.size());
        OrderItem orderItemOrder1 = orderItems.get(0);
        // Period 1 month from now 03.12.2021 - 03.01.2022
        Assertions.assertEquals(orderItemOrder1.getPeriodCount(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodFrequency(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodUnit(), Period.MONTHLY);

        // No payments yet
        Assertions.assertNull(order1.getEndDate());
        Assertions.assertNull(order1.getStartDate());

        // No subscriptions created yet for order
        Assertions.assertNull(order1.getSubscriptionId());

        LocalDateTime today = DateTimeUtil.getFormattedDateTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        String todayDateAsString = today.format(formatter);
        Assertions.assertEquals(todayDateAsString, todayDateAsString);

        // FIRST payment today -> 03.12.2021
        PaymentMessage firstPayment = PaymentMessage
                .builder()
                .paymentId("1")
                .orderId(order1Id)
                .namespace(order1.getNamespace())
                .userId(order1.getUser())
                .orderType(order1.getType())
                .paymentPaidTimestamp(today.toString())
                .build();

        // Mimic first payment event
        subscriptionController.paymentPaidEventCallback(firstPayment);

        // Refetch order1 from db
        order1 = orderService.findById(order1Id);
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));

        String oneMonthFromTodayMinusOneDay = today
                .plus(1, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS)
                .format(formatter);

        Assertions.assertEquals(oneMonthFromTodayMinusOneDay, order1.getEndDate().format(formatter));
        // Start date is payment paid timestamp
        Assertions.assertEquals(order1.getStartDate().format(formatter), todayDateAsString);

        String oneMonthFromToday = today
                .plus(1, ChronoUnit.MONTHS)
                .format(formatter);

        Assertions.assertEquals(nextDateCalculator.calculateNextDateTime(
                        today,
                        orderItemOrder1.getPeriodUnit(),
                        orderItemOrder1.getPeriodFrequency()).format(formatter),
                oneMonthFromToday);
        // Asserts that endDate is moved + 1 month and -1 day from today date.
        Assertions.assertEquals(oneMonthFromTodayMinusOneDay, order1.getEndDate().format(formatter));

        // Is subscription created
        Assertions.assertNotNull(order1.getSubscriptionId());

        String firstSubscriptionId = order1.getSubscriptionId();
        // Fetch subscription and return
        return subscriptionService.findById(firstSubscriptionId);

    }

    public String getFirstMerchantIdFromNamespace(String namespace) {

        String jsonResponse = restServiceClient.getClient().get()
                .uri(serviceUrls.getMerchantServiceUrl() + "/merchant/list-by-namespace?namespace=" + namespace)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.info(jsonResponse);
        JSONArray result = new JSONArray(jsonResponse);
        log.info(result.toString());
        return result.getJSONObject(0).getString("merchantId");
    }

    public String createMockProductMapping(String namespace, String merchantId) {

        String jsonResponse = restServiceClient.getClient().get()
                .uri(serviceUrls.getProductMappingServiceUrl() + "/create?namespace=" + namespace +"&namespaceEntityId=automatedtestproduct&merchantId=" + merchantId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.info(jsonResponse);
        JSONObject result = new JSONObject(jsonResponse);
        log.info(result.toString());
        return result.getString("productId");
    }

    public JSONObject createMockAccountingForProductId(String productId) throws JsonProcessingException {

        JSONObject productAccounting = new JSONObject();

        productAccounting.put("productId", productId);
        productAccounting.put("companyCode", "companyCode");
        productAccounting.put("mainLedgerAccount", "mainLedgerAccount");
        productAccounting.put("vatCode", "vatCode");
        productAccounting.put("internalOrder", "internalOrder");
        productAccounting.put("profitCenter", "profitCenter");
        productAccounting.put("balanceProfitCenter", "balanceProfitCenter");
        productAccounting.put("project", "project");
        productAccounting.put("operationArea", "operationArea");

        JSONObject jsonResponse = restServiceClient.makePostCall(
                serviceUrls.getProductServiceUrl() + "/product/" + productId + "/accounting",
                productAccounting.toString()
        );
        log.info(jsonResponse.toString());
        return jsonResponse;
    }

    protected ResponseEntity<JSONObject> createResolveProductResponse (){
        return createResolveProductResponse(true);
    }
    protected ResponseEntity<JSONObject> createResolveProductResponse (boolean includeMeta)
    {
        JSONObject ResolveProductResultDto = new JSONObject();
        ResolveProductResultDto.put("subscriptionId","dummyProductId");
        ResolveProductResultDto.put("userId","userId");
        ResolveProductResultDto.put("productId","newProductId");
        ResolveProductResultDto.put("productName","newProductName");
        ResolveProductResultDto.put("productLabel","newProductLabel");
        ResolveProductResultDto.put("productDescription","newProductDescription");

        if( includeMeta ) {
            JSONObject ResolveProductMetaDto1 = new JSONObject();
            ResolveProductMetaDto1.put("key", "key1");
            ResolveProductMetaDto1.put("value", "value1");
            ResolveProductMetaDto1.put("label", "label1");
            ResolveProductMetaDto1.put("visibleInCheckout", "true");
            ResolveProductMetaDto1.put("ordinal", "2");
            JSONObject ResolveProductMetaDto2 = new JSONObject();
            ResolveProductMetaDto2.put("key", "key2");
            ResolveProductMetaDto2.put("value", "value2");
            ResolveProductMetaDto2.put("label", "label2");
            ResolveProductMetaDto2.put("visibleInCheckout", "true");
            ResolveProductMetaDto2.put("ordinal", "1");
            Collection<JSONObject> orderItemMetas = new ArrayList<JSONObject>();
            orderItemMetas.add(ResolveProductMetaDto1);
            orderItemMetas.add(ResolveProductMetaDto2);
            ResolveProductResultDto.put("orderItemMetas", orderItemMetas);
        }

        return new ResponseEntity<>( ResolveProductResultDto, HttpStatus.OK);
    }

    protected ResponseEntity<JSONObject> createResolvePriceResponse()
    {
        JSONObject ResolvePriceResultDto = new JSONObject();
        ResolvePriceResultDto.put("subscriptionId","dummyProductId");
        ResolvePriceResultDto.put("userId","userId");
        ResolvePriceResultDto.put("priceNet","8");
        ResolvePriceResultDto.put("priceVat","2");
        ResolvePriceResultDto.put("priceGross","10");
        return new ResponseEntity<>( ResolvePriceResultDto, HttpStatus.OK);
    }

    public JSONObject createMockInvoiceAccountingForProductId(String productId) throws JsonProcessingException {

        JSONObject productAccounting = new JSONObject();

        productAccounting.put("productId", productId);
        productAccounting.put("salesOrg", "salesOrg");
        productAccounting.put("salesOffice", "salesOffice");
        productAccounting.put("material", "material");
        productAccounting.put("orderType", "orderType");

        JSONObject jsonResponse = restServiceClient.makePostCall(
                serviceUrls.getProductServiceUrl() + "/product/invoicing",
                productAccounting.toString()
        );
        log.info(jsonResponse.toString());
        return jsonResponse;
    }

    public TestPayment savePaytrailResponseAsPayment(TestPaytrailPaymentResponse paytrailResponse, Order order3) throws IOException {
        TestPayment payment = new TestPayment();
        payment.setPaytrailTransactionId(paytrailResponse.getTransactionId());
        payment.setPaymentId(order3.getOrderId());
        payment.setOrderId(order3.getOrderId());
        payment.setUserId(order3.getUser());
        payment.setStatus("payment_created");
        payment.setPaymentGateway("PAYTRAIL"); // Enum value is paytrail-online but in db it is PAYTRAIL
        payment.setTotal(new BigDecimal(order3.getPriceTotal()));
        payment.setTotalExclTax(new BigDecimal(order3.getPriceTotal()));
        payment.setNamespace(order3.getNamespace());
        payment.setDescription(log.getName());
        IndexResponse indexResponse = this.saveTestPayment(payment);
        return payment;
    }

    public void createPaytrailFormAndSubmitSuccessFullPayment(TestPaytrailPaymentResponse paytrailResponse, String providerId) {
        Map<String, String> formParams = PaytrailHelper.getFormParams(paytrailResponse, providerId);
        String actionUrl = PaytrailHelper.getFormAction(paytrailResponse, providerId);

        // STEP 3: Launch Playwright and submit the form
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(false) // set true to run without UI
            );

            Page page = browser.newPage();
            page.navigate("about:blank");

            // Build the form using JS
            StringBuilder js = new StringBuilder("const form = document.createElement('form');");
            js.append("form.method = 'POST';");
            js.append("form.action = '").append(actionUrl).append("';");
            js.append("form.style.display = 'none';");

            assert formParams != null;
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                js.append("{")
                        .append("let input = document.createElement('input');")
                        .append("input.type = 'hidden';")
                        .append("input.name = '").append(entry.getKey()).append("';")
                        .append("input.value = '").append(entry.getValue()).append("';")
                        .append("form.appendChild(input);")
                        .append("}");
            }


            js.append("document.body.appendChild(form); form.submit();");

            page.evaluate(js.toString());

            // Optional: wait to observe redirect
            page.waitForTimeout(5000);
        }
    }

    public IndexResponse saveTestPayment(TestPayment payment) throws IOException {
        // Convert Payment object to JSON
        String paymentJson = objectMapper.writeValueAsString(payment);

        // Create an IndexRequest for the specified index
        IndexRequest indexRequest = new IndexRequest("payments")
                .id(payment.getPaymentId()) // Use payment ID as document ID
                .source(paymentJson, XContentType.JSON);

        // Execute the index request
        IndexResponse indexResponse = this.clientResolver.get().index(indexRequest, RequestOptions.DEFAULT);

        // Force a refresh on the "payments" index to make the document immediately searchable
        this.clientResolver.get().indices().refresh(new RefreshRequest("payments"), RequestOptions.DEFAULT);

        // Return the response from indexing
        return indexResponse;
    }

    public IndexResponse createTestRefundPayment(TestRefundPayment payment) throws IOException {
        // Convert Payment object to JSON
        String paymentJson = objectMapper.writeValueAsString(payment);

        // Create an IndexRequest for the specified index
        IndexRequest indexRequest = new IndexRequest("refund_payments")
                .id(payment.getRefundId()) // Use payment ID as document ID
                .source(paymentJson, XContentType.JSON);

        // Execute the index request
        IndexResponse indexResponse = this.clientResolver.get().index(indexRequest, RequestOptions.DEFAULT);

        // Force a refresh on the "payments" index to make the document immediately searchable
        this.clientResolver.get().indices().refresh(new RefreshRequest("refund_payments"), RequestOptions.DEFAULT);

        // Return the response from indexing
        return indexResponse;
    }

    public IndexResponse createTestProductAccounting(TestProductAccounting payment) throws IOException {
        // Convert Payment object to JSON
        String paymentJson = objectMapper.writeValueAsString(payment);

        // Create an IndexRequest for the specified index
        IndexRequest indexRequest = new IndexRequest("accounting")
                .id(payment.getProductId()) // Use payment ID as document ID
                .source(paymentJson, XContentType.JSON);

        // Execute the index request
        IndexResponse indexResponse = this.clientResolver.get().index(indexRequest, RequestOptions.DEFAULT);

        // Force a refresh on the "accounting" index to make the document immediately searchable
        this.clientResolver.get().indices().refresh(new RefreshRequest("accounting"), RequestOptions.DEFAULT);

        // Return the response from indexing
        return indexResponse;
    }


    public JSONObject queryMailhoqMessages(){

        HttpClient httpClient = HttpClient.create();

        WebClient client = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // Set to 2 MB
                        .build())
                .build();

        String jsonResponse = client.get()
                .uri("http://localhost:8025/api/v2/messages")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (jsonResponse == null) {
            return new JSONObject();
        } else {
            return new JSONObject(jsonResponse);
        }
    }

    public Integer mailHoqMessageCount(){
        return Integer.parseInt(this.queryMailhoqMessages().get("total").toString());
    }

    /**
     * Filters messages based on a custom condition provided by the predicate.
     *
     * @param condition a Predicate that defines the filtering condition on each message
     * @return a list of messages that match the given condition
     */
    public List<JSONObject> filterMailhoqMessages(Predicate<JSONObject> condition) {
        List<JSONObject> matchingMessages = new ArrayList<>();

        // Get the "items" array from the JSON response
        JSONArray items = this.queryMailhoqMessages().getJSONArray("items");

        // Loop through each item in the array
        for (int i = 0; i < items.length(); i++) {
            JSONObject message = items.getJSONObject(i);

            // Apply the custom condition to determine if the message should be included
            if (condition.test(message)) {
                matchingMessages.add(message);
            }
        }

        return matchingMessages;
    }

    public String createNamespaceAndMerchantWithSecret(String namespace, String merchantName, String paytrailMerchantId, String paytrailSecret) {
        String baseUrl = serviceUrls.getMerchantServiceUrl();
        String merchantId = null;

        // STEP 1: Create Namespace
        try {
            JSONObject nsBody = new JSONObject();
            nsBody.put("namespace", namespace);

            JSONArray nsConfigs = new JSONArray();
            JSONObject nsConfigItem = new JSONObject();
            nsConfigItem.put("key", "customTestUtilValue");
            nsConfigItem.put("value", "true");
            nsConfigs.put(nsConfigItem);
            nsBody.put("configurations", nsConfigs);

            restServiceClient.makePostCall(baseUrl + "/namespace/create", nsBody.toString());
            log.info("✅ Namespace created or updated: {}", namespace);
        } catch (Exception e) {
            log.error("❌ Failed to create namespace [{}]: {}", namespace, e.getMessage());
        }

        // STEP 2: Check if merchant exists by namespace
        JSONObject existingMerchant = null;
        try {
            String findMerchantUrl = baseUrl + "/merchant/list-by-namespace?namespace=" + namespace;
            JSONArray list = restServiceClient.queryJsonArrayService(restServiceClient.getClient(), findMerchantUrl);

            for (int i = 0; i < list.length(); i++) {
                JSONObject merchant = list.getJSONObject(i);
                if (merchantName.equalsIgnoreCase(merchant.optString("merchantName"))) {
                    existingMerchant = merchant;
                    break;
                }
            }

            if (existingMerchant != null) {
                log.info("🔄 Existing merchant found, will update: {}", existingMerchant.optString("merchantId"));
            } else {
                log.info("🆕 No merchant found for namespace '{}', will create new", namespace);
            }
        } catch (Exception e) {
            log.error("❌ Failed to check existing merchant: {}", e.getMessage());
        }

        // STEP 4: Add Paytrail Secret
        try {
            if (existingMerchant != null) {
                String addSecretUrl = baseUrl + "/merchant/paytrail-secret/add"
                        + "?merchantId=" + existingMerchant
                        + "&secret=" + paytrailSecret;

                restServiceClient.makeGetCall(addSecretUrl);
                log.info("🔐 Paytrail secret added to merchant {}", existingMerchant);
            } else {
                log.warn("⚠️ Skipping Paytrail secret addition — merchantId is null");
            }
        } catch (Exception e) {
            log.error("❌ Failed to add Paytrail secret: {}", e.getMessage());
        }
        // STEP 3: Create or update merchant
        try {
            JSONObject merchantBody = new JSONObject();
            merchantBody.put("namespace", namespace);
            merchantBody.put("merchantName", merchantName);
//            merchantBody.put("merchantPaytrailMerchantId", paytrailMerchantId);

            if (existingMerchant != null) {
                merchantBody.put("merchantId", existingMerchant.optString("merchantId"));
            }

            JSONArray merchantConfigs = new JSONArray();
            JSONObject configItem = new JSONObject();
            configItem.put("key", "randomKeyForTesting");
            configItem.put("value", paytrailMerchantId);
            merchantConfigs.put(configItem);

            merchantBody.put("configurations", merchantConfigs);

            JSONObject merchantResponse = restServiceClient.makePostCall(baseUrl + "/merchant/upsert", merchantBody.toString());
            merchantId = merchantResponse.optString("merchantId", null);



            if (merchantId != null) {
                log.info("✅ Merchant upserted: {}", merchantId);
            } else {
                log.warn("⚠️ Merchant upsert returned null merchantId");
            }
        } catch (Exception e) {
            log.error("❌ Failed to upsert merchant: {}", e.getMessage());
        }

        // STEP 4: Add Paytrail Secret
        try {
            if (merchantId != null) {
                String addSecretUrl = baseUrl + "/merchant/paytrail-secret/add"
                        + "?merchantId=" + merchantId
                        + "&secret=" + paytrailSecret;

                restServiceClient.makeGetCall(addSecretUrl);
                log.info("🔐 Paytrail secret added to merchant {}", merchantId);

                JSONObject merchantBody = new JSONObject();
                merchantBody.put("namespace", namespace);
                merchantBody.put("merchantName", merchantName);
                merchantBody.put("merchantId", merchantId);
                JSONArray merchantUpdateConfigs = new JSONArray();
                JSONObject updateConfigItem = new JSONObject();
                updateConfigItem.put("key", MERCHANT_PAYTRAIL_MERCHANT_ID);
                updateConfigItem.put("value", paytrailMerchantId);
                merchantUpdateConfigs.put(updateConfigItem);

                merchantBody.put("configurations", merchantUpdateConfigs);

                JSONObject merchantUpdateResponse = restServiceClient.makePostCall(baseUrl + "/merchant/upsert", merchantBody.toString());

            } else {
                log.warn("⚠️ Skipping Paytrail secret addition — merchantId is null");
            }
        } catch (Exception e) {
            log.error("❌ Failed to add Paytrail secret: {}", e.getMessage());
        }

        return merchantId;
    }
}
