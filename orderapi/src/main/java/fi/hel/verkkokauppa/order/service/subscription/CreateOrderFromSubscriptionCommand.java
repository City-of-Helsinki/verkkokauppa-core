package fi.hel.verkkokauppa.order.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.subscription.outbound.SubscriptionPriceResultDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemMetaTransformer;
import fi.hel.verkkokauppa.order.api.request.rightOfPurchase.SubscriptionPriceRequest;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionItemMeta;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemMetaRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionItemMetaRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.order.OrderItemMetaService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.rightOfPurchase.OrderRightOfPurchaseService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class CreateOrderFromSubscriptionCommand {

    private Logger log = LoggerFactory.getLogger(CreateOrderFromSubscriptionCommand.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private RestServiceClient customerApiCallService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemMetaService orderItemMetaService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemMetaTransformer orderItemMetaTransformer;

    @Autowired
    private OrderItemMetaRepository orderItemMetaRepository;

    @Autowired
    private SubscriptionItemMetaRepository subscriptionItemMetaRepository;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public String createFromSubscription(SubscriptionDto subscriptionDto) {
        String namespace = subscriptionDto.getNamespace();
        String user = subscriptionDto.getUser();

        String subscriptionId = subscriptionDto.getSubscriptionId();

        String activeOrderFromSubscription = hasDuplicateOrder(subscriptionId, user);

        if (activeOrderFromSubscription != null) {
            return activeOrderFromSubscription;
        }

        boolean hasRightToPurchase = orderService.validateRightOfPurchase(subscriptionDto.getOrderId(), user, namespace);
        // Returns null orderId if subscription right of purchase is false.
        if (!hasRightToPurchase) {
            log.info("subscription-renewal-no-right-of-purchase {}", subscriptionDto.getSubscriptionId());
            return null;
        }

        Order order = orderService.createByParams(namespace, user);
        order.setType(OrderType.ORDER);
        Subscription subscription = getSubscriptionQuery.findByIdValidateByUser(subscriptionDto.getSubscriptionId(), user);

        subscription = updateSubscriptionPricesFromMerchant(subscriptionDto, namespace, user, subscriptionId, subscription);

        orderService.setStartDateAndCalculateNextEndDate(order, subscription, subscription.getEndDate());
        copyCustomerInfoFromSubscription(subscriptionDto, order);
        orderService.setTotals(order, subscriptionDto.getPriceNet(), subscriptionDto.getPriceVat(), subscriptionDto.getPriceGross());

        String orderId = order.getOrderId();
        String orderItemId = createOrderItemFieldsFromSubscription(orderId, subscriptionDto);
        copyOrderItemMetaFieldsFromSubscription(subscriptionDto, orderId, orderItemId);

        orderService.confirm(order);
        orderRepository.save(order);

        orderService.linkToSubscription(orderId, order.getUser(), subscriptionDto.getSubscriptionId());

        return orderId;
    }

    private Subscription updateSubscriptionPricesFromMerchant(SubscriptionDto subscriptionDto, String namespace, String user, String subscriptionId, Subscription subscription) {

        SubscriptionPriceRequest request = new SubscriptionPriceRequest();
        request.setSubscriptionId(subscriptionId);
        request.setNamespace(namespace);
        request.setUserId(user);
        ResponseEntity<JSONObject> response;
        SubscriptionPriceResultDto resultDto;
        try {
            response = customerApiCallService.postCall(request, ServiceConfigurationKeys.SUBSCRIPTION_PRICE_URL, namespace);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Exception("Price call failed request: " + request);
            }
            resultDto = objectMapper.readValue(Objects.requireNonNull(response.getBody()).toString(), SubscriptionPriceResultDto.class);
            if (resultDto.getSubscriptionId() != null && resultDto.getUserId() != null) {
                // Fetch subscription.
                subscription = getSubscriptionQuery.findByIdValidateByUser(resultDto.getSubscriptionId(), resultDto.getUserId());
                log.info("Old prices for subscription: {} getPriceNet:{} getPriceVat: {} getPriceGross: {}",
                        subscription.getSubscriptionId(),
                        subscription.getPriceNet(),
                        subscription.getPriceVat(),
                        subscription.getPriceGross()
                        );
                // Set new prices to subscription from result.
                subscription.setPriceNet(resultDto.getPriceNet());
                subscription.setPriceVat(resultDto.getPriceVat());
                subscription.setPriceGross(resultDto.getPriceGross());
                log.info("New prices for subscription: {} getPriceNet:{} getPriceVat: {} getPriceGross: {}",
                        resultDto.getSubscriptionId(),
                        resultDto.getPriceNet(),
                        resultDto.getPriceVat(),
                        resultDto.getPriceGross()
                );
                // Save given subscription values to database
                subscription = subscriptionRepository.save(subscription);
                // Update dto prices from updated price fields from subscription because
                // we want new order to be made with new prices in later steps.
                subscriptionDto.setPriceNet(subscription.getPriceNet());
                subscriptionDto.setPriceVat(subscription.getPriceVat());
                subscriptionDto.setPriceGross(subscription.getPriceGross());
            }
        } catch (Exception e) {
            log.error("Error/new price not found when requesting new price to subscription request:{}", request, e);
        }
        return subscription;
    }

    public String hasDuplicateOrder(String subscriptionId, String user) {
        try {

            Subscription subscription = getSubscriptionQuery.findByIdValidateByUser(subscriptionId, user);
            Order lastOrder = orderService.getLatestOrderWithSubscriptionId(subscriptionId);
            if (lastOrder != null) {
                // order endDate greater than current subscription endDate
                if (hasActiveSubscriptionOrder(subscription, lastOrder)) {
                    return lastOrder.getOrderId();
                }
            }
            return null;
        } catch (Exception e) {
            //
            log.info(String.valueOf(e));
            return null;
        }
    }

    public boolean hasActiveSubscriptionOrder(Subscription subscription, Order lastOrder) {
        LocalDateTime subscriptionEndDate = subscription.getEndDate();
        LocalDateTime lastOrderEndDate = lastOrder.getEndDate();
        // Prevents NPO
        if (subscriptionEndDate == null || lastOrderEndDate == null) {
            return false;
        }

        // order endDate greater than current subscription endDate (isAfter)
        return lastOrderEndDate.isAfter(subscriptionEndDate);
    }

    private void copyCustomerInfoFromSubscription(SubscriptionDto subscriptionDto, Order order) {
        String customerFirstName = subscriptionDto.getCustomerFirstName();
        String customerLastName = subscriptionDto.getCustomerLastName();
        String customerEmail = subscriptionDto.getCustomerEmail();
        String customerPhone = subscriptionDto.getCustomerPhone();
        orderService.setCustomer(order, customerFirstName, customerLastName, customerEmail, customerPhone);
    }

    private String createOrderItemFieldsFromSubscription(String orderId, SubscriptionDto subscriptionDto) {
        return orderItemService.addItem(
                orderId,
                subscriptionDto.getProductId(),
                subscriptionDto.getProductName(),
                subscriptionDto.getQuantity(),
                subscriptionDto.getUnit(),
                subscriptionDto.getPriceNet(),
                subscriptionDto.getPriceVat(),
                subscriptionDto.getPriceGross(),
                subscriptionDto.getVatPercentage(),
                subscriptionDto.getPriceNet(),
                subscriptionDto.getPriceVat(),
                subscriptionDto.getPriceGross(),
                null,
                null,
                null,
                subscriptionDto.getBillingStartDate(),
                subscriptionDto.getEndDate()
        );

    }

    private void copyOrderItemMetaFieldsFromSubscription(SubscriptionDto subscriptionDto, String orderId, String orderItemId) {
        List<SubscriptionItemMeta> subscriptionMeta = subscriptionItemMetaRepository.findBySubscriptionId(subscriptionDto.getSubscriptionId());
        if (!subscriptionMeta.isEmpty()) {
            subscriptionMeta.forEach(meta -> {
                OrderItemMetaDto orderItemMeta = new OrderItemMetaDto(
                        null,
                        orderItemId,
                        orderId,
                        meta.getKey(),
                        meta.getValue(),
                        meta.getLabel(),
                        meta.getVisibleInCheckout(),
                        meta.getOrdinal()
                );

                String createdOrderItemMetaId = orderItemMetaService.addItemMeta(orderItemMeta);

                log.debug("created new orderItemMeta " + createdOrderItemMetaId + " from subscriptionItemMeta: " + meta.getOrderItemMetaId());
            });
        }
    }

}
