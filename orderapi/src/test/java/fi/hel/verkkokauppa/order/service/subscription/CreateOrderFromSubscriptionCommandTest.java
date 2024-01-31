package fi.hel.verkkokauppa.order.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.id.IncrementId;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.admin.SubscriptionAdminController;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemMetaRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.renewal.SubscriptionRenewalService;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Slf4j
@RunIfProfile(profile = "local")
class CreateOrderFromSubscriptionCommandTest extends TestUtils {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderItemMetaRepository orderItemMetaRepository;
    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionAdminController subscriptionAdminController;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private CreateOrderFromSubscriptionCommand createOrderFromSubscriptionCommand;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

    @Autowired
    private IncrementId incrementId;

    @MockBean
    private SubscriptionRenewalService renewalServiceMock;

    @MockBean
    private RestServiceClient restServiceClientMock;

    @MockBean
    private OrderService orderServiceMock;

    private ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
    private ArrayList<String> toBeDeletedSubscriptionById = new ArrayList<>();



    private String mailHogUrl = "http://localhost:8025";

    @Test
    public void assertTrue() {
        Assertions.assertTrue(true);
    }

    @AfterEach
    public void tearDown() {
        try {
            toBeDeletedOrderById.forEach(orderId -> orderRepository.deleteById(orderId));
            toBeDeletedSubscriptionById.forEach(subscriptionId -> subscriptionRepository.deleteById(subscriptionId));

            // Clear list because all deleted
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedSubscriptionById = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedSubscriptionById = new ArrayList<>();

        }
    }


    @Test
    @RunIfProfile(profile = "local")
    void testResolveProductAndResolvePrice() throws InterruptedException, JsonProcessingException {
        ReflectionTestUtils.setField(createOrderFromSubscriptionCommand, "restServiceClient", restServiceClientMock);
        ReflectionTestUtils.setField(createOrderFromSubscriptionCommand, "customerApiCallService", restServiceClientMock);
        ReflectionTestUtils.setField(createOrderFromSubscriptionCommand, "getSubscriptionQuery", getSubscriptionQuery);
        ReflectionTestUtils.setField(createOrderFromSubscriptionCommand, "subscriptionRepository", subscriptionRepository);
        ReflectionTestUtils.setField(createOrderFromSubscriptionCommand, "orderService", orderServiceMock);
        ReflectionTestUtils.setField(createOrderFromSubscriptionCommand, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(orderServiceMock, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(orderServiceMock, "incrementId", incrementId);
        ReflectionTestUtils.setField(orderServiceMock, "log", log);

        when(orderServiceMock.validateRightOfPurchase(any(), any(), any())).thenReturn(true);
        when(orderServiceMock.createByParams(any(), any())).thenCallRealMethod();

        // create order and subscription for test
        Order order = generateDummyOrder();
        order.setOrderId(UUID.randomUUID().toString());
//        order.setEndDate("");
        order.setNamespace("venepaikat");
        order = orderRepository.save(order);

        toBeDeletedOrderById.add(order.getOrderId());
        log.info("Order id: " +order.getOrderId());

        Subscription subscription = generateDummySubscription(order);
        subscription.setSubscriptionId(UUID.randomUUID().toString());
        subscription.setNamespace("venepaikat");
        subscription.setProductId("dummyProductId");
        subscription.setProductName("productName");
        subscription.setProductDescription("productDescription");
        subscription.setProductLabel("productLabel");
        subscriptionRepository.save(subscription);
        toBeDeletedSubscriptionById.add(subscription.getSubscriptionId());

        JSONObject productMapping = new JSONObject();
        productMapping.put("productId","dummyProductId");
        productMapping.put("namespace","venepaikat");
        productMapping.put("namespaceEntityId","namespaceEntityId");
        productMapping.put("merchantId","merchantId");
        when(restServiceClientMock.makeGetCall(any())).thenReturn(productMapping);

        ResponseEntity<JSONObject> resolveProductResponse = createResolveProductResponse();

        ResponseEntity<JSONObject> resolvePriceResponse = createResolvePriceResponse();

        when(restServiceClientMock.postCall(any(), any(), any())).thenReturn(resolveProductResponse).thenReturn(resolvePriceResponse);

        SubscriptionMessage subscriptionMessage = SubscriptionMessage.builder()
                .eventType(EventType.SUBSCRIPTION_RENEWAL_REQUESTED)
                .timestamp(DateTimeUtil.getDateTime())
                .subscriptionId(subscription.getSubscriptionId())
                .namespace(subscription.getNamespace())
                .eventTimestamp(DateTimeUtil.getDateTime())
                .build();

        SubscriptionDto subscriptionDto = getSubscriptionQuery.mapToDto(subscription);
        String orderId = createOrderFromSubscriptionCommand.createFromSubscription(subscriptionDto);

        Optional<Subscription> returnedOptional = subscriptionRepository.findById(subscription.getSubscriptionId());
        Subscription updatedSubscription = returnedOptional.get();
        Assert.assertNotEquals(subscription.getProductId(), updatedSubscription.getProductId());
        Assert.assertNotEquals(subscription.getProductLabel(), updatedSubscription.getProductLabel());
        Assert.assertNotEquals(subscription.getProductName(), updatedSubscription.getProductName());
        Assert.assertNotEquals(subscription.getProductDescription(), updatedSubscription.getProductDescription());
        Assert.assertNotEquals(subscription.getPriceVat(), updatedSubscription.getPriceVat());
        Assert.assertNotEquals(subscription.getPriceNet(), updatedSubscription.getPriceNet());
        Assert.assertNotEquals(subscription.getPriceGross(), updatedSubscription.getPriceGross());

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        Assert.assertNotNull(orderItems);
        List<OrderItemMeta> orderItemMetas = orderItemMetaRepository.findByOrderId(orderId);

        for(int i=0; i < orderItemMetas.size(); i++)
        {
            OrderItemMeta itemMeta = orderItemMetas.get(i);
            if( itemMeta.getLabel().equals("label1") ){
                Assert.assertEquals(itemMeta.getKey(), "key1");
                Assert.assertEquals(itemMeta.getLabel(), "label1");
                Assert.assertEquals(itemMeta.getOrdinal(), "2");
                Assert.assertEquals(itemMeta.getValue(), "value1");
                Assert.assertEquals(itemMeta.getVisibleInCheckout(), "true");
            }
            else {
                Assert.assertEquals(itemMeta.getKey(), "key2");
                Assert.assertEquals(itemMeta.getLabel(), "label2");
                Assert.assertEquals(itemMeta.getOrdinal(), "1");
                Assert.assertEquals(itemMeta.getValue(), "value2");
                Assert.assertEquals(itemMeta.getVisibleInCheckout(), "true");
            }

        }
    }
}