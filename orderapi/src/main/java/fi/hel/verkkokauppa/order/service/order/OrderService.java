package fi.hel.verkkokauppa.order.service.order;

import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.history.factory.HistoryFactory;
import fi.hel.verkkokauppa.common.history.util.HistoryUtil;
import fi.hel.verkkokauppa.common.id.IncrementId;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.ListUtil;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.CustomerDto;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformerUtils;
import fi.hel.verkkokauppa.order.logic.subscription.NextDateCalculator;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.OrderStatus;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.service.rightOfPurchase.OrderRightOfPurchaseService;
import fi.hel.verkkokauppa.order.service.subscription.GetSubscriptionQuery;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class OrderService {

    private Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemMetaService orderItemMetaService;

    @Autowired
    private OrderTransformerUtils orderTransformerUtils;

    @Autowired
    private NextDateCalculator nextDateCalculator;

    @Autowired
    private SendEventService sendEventService;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

    @Autowired
    private OrderRightOfPurchaseService orderRightOfPurchaseService;

    @Autowired
    private HistoryFactory historyFactory;
    @Autowired
    private HistoryUtil historyUtil;

    @Autowired
    private RestServiceClient restServiceClient;
    @Autowired
    private ServiceUrls serviceUrl;

    @Autowired
    private IncrementId incrementId;

    public ResponseEntity<OrderAggregateDto> orderAggregateDto(String orderId) {
        OrderAggregateDto orderAggregateDto = getOrderWithItems(orderId);

        if (orderAggregateDto == null) {
            Error error = new Error("order-not-found-from-backend", "order with id [" + orderId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        return ResponseEntity.ok().body(orderAggregateDto);
    }

    public OrderAggregateDto getOrderWithItems(final String orderId) {
        OrderAggregateDto orderAggregateDto = null;
        Order order = findById(orderId);

        if (order != null) {
            List<OrderItem> items = this.orderItemService.findByOrderId(orderId);
            List<OrderItemMeta> metas = this.orderItemMetaService.findByOrderId(orderId);
            orderAggregateDto = orderTransformerUtils.transformToOrderAggregateDto(order, items, metas);
        }

        return orderAggregateDto;
    }

    public String generateOrderId(String namespace, String user, LocalDateTime timestamp) {
        String whoseOrder = UUIDGenerator.generateType3UUIDString(namespace, user);
        String orderId = UUIDGenerator.generateType3UUIDString(whoseOrder, timestamp.toString());
        return orderId;
    }

    public Order createByParams(String namespace, String user) {
        LocalDateTime createdAt = DateTimeUtil.getFormattedDateTime();
        String orderId = generateOrderId(namespace, user, createdAt);
        Long incrementId = this.incrementId.generateOrderIncrementId();
        Order order = new Order(orderId, namespace, user, createdAt, incrementId);

        orderRepository.save(order);
        log.debug("created new order, orderId: " + orderId);
        return order;
    }

    public Order findById(String orderId) {
        Optional<Order> mapping = orderRepository.findById(orderId);

        if (mapping.isPresent())
            return mapping.get();

        log.warn("order not found, orderId: " + orderId);
        return null;
    }

    public List<Order> findByUser(String userId) {
        return orderRepository.findByUser(userId);
    }

    public Order findByNamespaceAndUser(String namespace, String user) {
        List<Order> matchingOrders = orderRepository.findByNamespaceAndUser(namespace, user);

        if (matchingOrders.size() > 0)
            return matchingOrders.get(0);

        log.debug("order not found, namespace: " + namespace + " user: " + user);
        return null;
    }


    public void setCustomer(String orderId, CustomerDto customerDto) {
        Order order = findById(orderId);
        if (order != null)
            setCustomer(order, customerDto.getCustomerFirstName(), customerDto.getCustomerLastName(), customerDto.getCustomerEmail(), customerDto.getCustomerPhone());
    }

    public void setCustomer(Order order, CustomerDto customerDto) {
        setCustomer(order, customerDto.getCustomerFirstName(), customerDto.getCustomerLastName(), customerDto.getCustomerEmail(), customerDto.getCustomerPhone());
    }

    public void setCustomer(Order order, String customerFirstName, String customerLastName, String customerEmail, String customerPhone) {
        order.setCustomerFirstName(customerFirstName);
        order.setCustomerLastName(customerLastName);
        order.setCustomerEmail(customerEmail);
        order.setCustomerPhone(customerPhone);

        orderRepository.save(order);
        log.debug("saved order customer details, orderId: " + order.getOrderId());
    }

    public void setTotals(String orderId, String priceNet, String priceVat, String priceTotal) {
        Order order = findById(orderId);
        if (order != null)
            setTotals(order, priceNet, priceVat, priceTotal);
    }

    public void setTotals(Order order, String priceNet, String priceVat, String priceTotal) {
        order.setPriceNet(priceNet);
        order.setPriceVat(priceVat);
        order.setPriceTotal(priceTotal);

        orderRepository.save(order);
        log.debug("saved order price totals, orderId: " + order.getOrderId());
    }

    public void markAsAccounted(String orderId) {
        Order order = findById(orderId);
        order.setAccounted(LocalDate.now());
        orderRepository.save(order);
        log.debug("marked order accounted, orderId: " + order.getOrderId());
    }

    public void setType(Order order, String type) {
        order.setType(type);

        orderRepository.save(order);
        log.debug("set order type, orderId: " + order.getOrderId() + " type: " + order.getType());
    }

    public void confirm(Order order) {
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.debug("confirmed order, orderId: " + order.getOrderId());
    }

    public void cancel(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        triggerOrderCancelledEvent(order);
        log.debug("canceled order, orderId: " + order.getOrderId());
    }

    public void triggerOrderCancelledEvent(Order order) {
        OrderMessage orderMessage = OrderMessage.builder()
                .eventType(EventType.ORDER_CANCELLED)
                .namespace(order.getNamespace())
                .orderId(order.getOrderId())
                .userId(order.getUser())
                .timestamp(DateTimeUtil.getDateTime())
                .build();

        sendEventService.sendEventMessage(TopicName.ORDERS, orderMessage);
        log.debug("triggered event " + EventType.ORDER_CANCELLED + " for orderId: " + order.getOrderId());
    }

    public Order findByIdValidateByUser(String orderId, String userId) {
        if (StringUtils.isEmpty(orderId) || StringUtils.isEmpty(userId)) {
            log.error("unauthorized attempt to load order, orderId or userId missing");
            Error error = new Error("order-not-found-from-backend", "order with id [" + orderId + "] and user id [" + userId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        Order order = findById(orderId);

        if (order == null) {
            Error error = new Error("order-not-found-from-backend", "order with id [" + orderId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        String orderUserId = order.getUser();
        if (orderUserId == null || userId == null || !orderUserId.equals(userId)) {
            log.error("unauthorized attempt to load order, userId does not match");
            Error error = new Error("order-not-found-from-backend", "order with order id [" + orderId + "] and user id [" + userId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        return order;
    }

    public void setOrderStartAndEndDate(Order order, Subscription subscription, PaymentMessage message) {
        LocalDateTime startDate = subscription.getStartDate();
        LocalDateTime paymentAt = DateTimeUtil.fromFormattedDateTimeString(message.getPaymentPaidTimestamp());

        if (startDate.isBefore(paymentAt)) {
            setStartDateAndCalculateNextEndDate(order, subscription, paymentAt);
        } else {
            setStartDateAndCalculateNextEndDate(order, subscription, startDate);
        }

        orderRepository.save(order);
    }

    public void setStartDateAndCalculateNextEndDate(Order order, Subscription subscription, LocalDateTime startDate) {
        order.setStartDate(startDate);

        // Order end date = start date + subscription cycle eg month
        LocalDateTime endDateAddedPeriodCycle = nextDateCalculator.calculateNextDateTime(
                startDate,
                subscription.getPeriodUnit(),
                subscription.getPeriodFrequency());

        LocalDateTime newEndDate = nextDateCalculator.calculateNextEndDateTime(
                endDateAddedPeriodCycle,
                subscription.getPeriodUnit()
        );
        order.setEndDate(newEndDate);
    }

    /**
     * Start datetime: Previous order enddate + 1 day(start of the day)
     * End datetime: start datetime + 1 month - 1 day(end of the day)
     */
    public void setStartDateAndCalculateNextEndDateAfterRenewal(Order order, Subscription subscription, LocalDateTime startDate) {
        // Previous order enddate = subscription endDate (passed to startDate)
        startDate = startDate.plus(1, ChronoUnit.DAYS);
        LocalDateTime startOfTheStartDate = startDate.with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay());

        order.setStartDate(startOfTheStartDate);

        // Order end date = start date + subscription cycle eg month
        LocalDateTime endDateAddedPeriodCycle = nextDateCalculator.calculateNextDateTime(
                startOfTheStartDate,
                subscription.getPeriodUnit(),
                subscription.getPeriodFrequency());
        // End datetime: start datetime + 1 month - 1day(end of the day)
        LocalDateTime newEndDate = nextDateCalculator.calculateNextEndDateTime(
                endDateAddedPeriodCycle,
                subscription.getPeriodUnit()
        );

        order.setEndDate(newEndDate);
    }


    public void linkToSubscription(String orderId, String userId, String subscriptionId) {
        Order order = findByIdValidateByUser(orderId, userId);
        order.setSubscriptionId(new ArrayList<>() {{
            add(subscriptionId);
        }});
        orderRepository.save(order);
    }

    public void triggerOrderCreatedEvent(Order order, String eventType) {
        OrderMessage.OrderMessageBuilder orderMessageBuilder = OrderMessage.builder()
                .eventType(eventType)
                .namespace(order.getNamespace())
                .orderId(order.getOrderId())
                .timestamp(DateTimeUtil.getFormattedDateTime(order.getCreatedAt()))
                .orderType(order.getType())
                .priceTotal(order.getPriceTotal())
                .priceNet(order.getPriceNet())
                .priceVat(order.getPriceVat());

        if (!order.getSubscriptionIds().isEmpty()) {
            SubscriptionDto subscription = getSubscriptionQuery.getOne(order.getSubscriptionId().stream().findFirst().orElse(""));
            String paymentMethodToken = subscription.getPaymentMethodToken();

            List<OrderItem> orderitems = orderItemService.findByOrderId(order.getOrderId());
            OrderItem orderItem = orderitems.get(0); // orders created from subscription have only one item

            orderMessageBuilder
                    .cardToken(paymentMethodToken)
                    .orderItemId(orderItem.getOrderItemId())
                    .vatPercentage(orderItem.getVatPercentage())
                    .productName(orderItem.getProductName())
                    .productQuantity(orderItem.getQuantity().toString())
                    .isSubscriptionRenewalOrder(true)
                    .subscriptionId(order.getSubscriptionId().stream().findFirst().orElse(""))
                    .userId(order.getUser());
        }

        OrderMessage orderMessage = orderMessageBuilder.build();
        sendEventService.sendEventMessage(TopicName.ORDERS, orderMessage);
        log.debug("triggered event " + eventType + " for orderId: " + order.getOrderId());
    }

    public List<OrderAggregateDto> findBySubscription(String subscriptionId) {
        List<Order> orderIds = orderRepository.findOrdersBySubscriptionId(subscriptionId);

        List<OrderAggregateDto> subscriptionOrders = orderIds.stream()
                .map(order -> getOrderWithItems(order.getOrderId()))
                .distinct()
                .sorted(Comparator.comparing(o -> o.getOrder().getCreatedAt()))
                .collect(Collectors.toList());

        return subscriptionOrders;
    }

    public List<OrderAggregateDto> findBySubscriptions(String subscriptionId) {
        List<Order> orderIds = orderRepository.findAllBySubscriptionIdsIn(Collections.singleton(new ArrayList<>() {{
            add(subscriptionId);
        }}));


        List<OrderAggregateDto> subscriptionOrders = orderIds.stream()
                .map(order -> getOrderWithItems(order.getOrderId()))
                .distinct()
                .sorted(Comparator.comparing(o -> o.getOrder().getCreatedAt()))
                .collect(Collectors.toList());

        return subscriptionOrders;
    }


    public boolean validateRightOfPurchase(String orderId, String user, String namespace) {
        Order order = findByIdValidateByUser(orderId, user);
        orderRightOfPurchaseService.setNamespace(namespace);
        OrderAggregateDto dto = getOrderWithItems(order.getOrderId());
        ResponseEntity<Boolean> canPurchase = orderRightOfPurchaseService.canPurchase(dto);
        return Boolean.TRUE.equals(canPurchase.getBody());
    }

    public Order getLatestOrderWithSubscriptionId(String subscriptionId) {
        List<OrderAggregateDto> orders = findBySubscription(subscriptionId);

        Optional<OrderAggregateDto> lastOrder = ListUtil.last(orders);
        return lastOrder.map(orderAggregateDto -> findById(orderAggregateDto.getOrder().getOrderId())).orElse(null);
    }

    public JSONObject saveOrderMessageHistory(OrderMessage message) {
        try {
            String request = historyUtil.toString(historyFactory.fromOrderMessage(message));
            return restServiceClient.makePostCall(serviceUrl.getHistoryServiceUrl() + "/history/create", request);
        } catch (Exception e) {
            log.error("saveOrderMessageHistory processing error: " + e.getMessage());
        }
        return null;
    }

    public JSONObject savePaymentMessageHistory(PaymentMessage message) {
        try {
            String request = historyUtil.toString(historyFactory.fromPaymentMessage(message));
            return restServiceClient.makePostCall(serviceUrl.getHistoryServiceUrl() + "/history/create", request);
        } catch (Exception e) {
            log.info("savePaymentMessageHistory processing error: " + e.getMessage());
        }
        return null;
    }

}
