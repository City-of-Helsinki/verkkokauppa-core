package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.rest.RestWebHookService;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.order.api.data.CustomerDto;
import fi.hel.verkkokauppa.order.api.data.FlowStepDto;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.order.api.data.TotalsDto;
import fi.hel.verkkokauppa.order.api.data.invoice.InvoiceDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformer;
import fi.hel.verkkokauppa.order.logic.OrderTypeLogic;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderStatus;
import fi.hel.verkkokauppa.order.service.CommonBeanValidationService;
import fi.hel.verkkokauppa.order.service.invoice.InvoiceService;
import fi.hel.verkkokauppa.order.service.order.FlowStepService;
import fi.hel.verkkokauppa.order.service.order.OrderItemMetaService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import fi.hel.verkkokauppa.order.service.order.OrderPaymentMethodService;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.rightOfPurchase.OrderRightOfPurchaseService;
import fi.hel.verkkokauppa.order.service.subscription.SubscriptionService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
public class OrderController {

	private Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private FlowStepService flowStepService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemMetaService orderItemMetaService;

	@Autowired
	private OrderTypeLogic orderTypeLogic;

	@Autowired
    private CommonBeanValidationService commonBeanValidationService;

    @Autowired
    private RestWebHookService restWebHookService;

    @Autowired
    private SubscriptionService subscriptionService;

	@Autowired
    private OrderRightOfPurchaseService orderRightOfPurchaseService;

    @Autowired
    private OrderPaymentMethodService orderPaymentMethodService;

    @Autowired
    private OrderTransformer orderTransformer;

    @GetMapping(value = "/order/create", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> createOrder(@RequestParam(value = "namespace") String namespace,
                                                         @RequestParam(value = "user") String user,
                                                         @RequestParam(required = false, value = "lastValidPurchaseDateTime") String lastValidPurchaseDateTime
    ) {
        try {
            LocalDateTime formattedLastValidPurchaseDateTime = null;
            if (lastValidPurchaseDateTime != null) {
                formattedLastValidPurchaseDateTime = DateTimeUtil.fromFormattedDateTimeOptionalString(lastValidPurchaseDateTime);
            }
            Order order = orderService.createByParams(namespace, user, formattedLastValidPurchaseDateTime);
            String orderId = order.getOrderId();
            return orderAggregateDto(orderId);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("creating order failed", e);
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, new Error("failed-to-create-order", "failed to create order"));
        }
	}

    @GetMapping(value = "/order/get", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> getOrder(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "userId") String userId) {
        try {
            orderService.findByIdValidateByUser(orderId, userId);
            return orderAggregateDto(orderId);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting order failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-order", "failed to get order with id [" + orderId + "]")
            );
        }
    }

    @GetMapping(value = "/order/get-by-subscription", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderAggregateDto>> getOrdersBySubscription(@RequestParam(value = "subscriptionId") String subscriptionId, @RequestParam(value = "userId") String userId) {
        try {
            subscriptionService.findByIdValidateByUser(subscriptionId, userId);
            List<OrderAggregateDto> subscriptionOrders = orderService.findBySubscription(subscriptionId);
            return ResponseEntity.ok(subscriptionOrders);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting orders failed, subscriptionId: " + subscriptionId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-orders", "failed to get orders with subscriptionId [" + subscriptionId + "]")
            );
        }
    }

    @GetMapping(value = "/order/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> confirmOrder(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "userId") String userId) {
        try {
            Order order = orderService.findByIdValidateByUser(orderId, userId);

            validateCustomerData(order.getCustomerFirstName(), order.getCustomerLastName(), order.getCustomerEmail(), order.getCustomerPhone());
            validateOrderTotalsExist(order);

            orderService.confirm(order);
            return orderAggregateDto(orderId);

        } catch (ConstraintViolationException cve) {
            log.warn("confirming invalid order rejected, orderId: " + orderId, cve);
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("rejected-confirming-invalid-order", "rejected confirming invalid order with id [" + orderId + "]")
            );
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("confirming order failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-confirm-order", "failed to confirm order with id [" + orderId + "]")
            );
        }
    }

    @GetMapping(value = "/order/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> cancelOrder(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "userId") String userId) {
        try {
            Order order = orderService.findByIdValidateByUser(orderId, userId);

            orderService.cancel(order);
            return orderAggregateDto(orderId);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("canceling order failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-cancel-order", "failed to cancel order with id [" + orderId + "]")
            );
        }
    }

    @GetMapping(value = "/order/right-of-purchase", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> rightOfPurchaseOrder(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "userId") String userId) {
        try {
            Order order = orderService.findByIdValidateByUser(orderId, userId);
            orderRightOfPurchaseService.setNamespace(order.getNamespace());
            OrderAggregateDto dto = orderService.getOrderWithItems(orderId);
            return orderRightOfPurchaseService.canPurchase(dto);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("right of purchase order failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-cancel-order", "failed to check right of purchase order id [" + orderId + "]")
            );
        }
    }

    @PostMapping(value = "/order/setCustomer", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> setCustomer(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "userId") String userId,
                                                         @RequestParam(value = "customerFirstName") String customerFirstName, @RequestParam(value = "customerLastName") String customerLastName,
                                                         @RequestParam(value = "customerEmail") String customerEmail, @RequestParam(value = "customerPhone") String customerPhone) {
        try {
            Order order = orderService.findByIdValidateByUser(orderId, userId);

            if (!changesToOrderAllowed(order)) {
                log.warn("setting customer to order rejected, orderId: " + orderId);
                throw new CommonApiException(
                        HttpStatus.FORBIDDEN,
                        new Error("rejected-changes-to-order", "rejected changes to order with id [" + orderId + "]")
                );
            }

            CustomerDto customerDto = validateCustomerData(customerFirstName, customerLastName, customerEmail, customerPhone);
            orderService.setCustomer(order, customerDto);
            return orderAggregateDto(orderId);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("setting order customer failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-set-order-customer", "failed to set customer for order with id [" + orderId + "]")
            );
        }
    }


    @PostMapping(value = "/order/setInvoice", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderAggregateDto> setInvoice(
            @RequestBody InvoiceDto invoice
    ) {
        String orderId = null;
        try {
            orderId = invoice.getOrderId();
            Order order = orderService.findByIdValidateByUser(orderId, invoice.getUserId());

            if (!changesToOrderAllowed(order) || Objects.equals(order.getType(), OrderType.SUBSCRIPTION)) {
                log.warn("setting invoice to order rejected, orderId: " + orderId);
                throw new CommonApiException(
                        HttpStatus.FORBIDDEN,
                        new Error("rejected-changes-to-order", "rejected changes to order with id [" + orderId + "], setting invoice to order rejected")
                );
            }

            invoiceService.saveInvoiceToOrder(invoice, order);

            return orderAggregateDto(orderId);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("setting order invoice failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-set-order-invoice", "failed to set invoice for order with id [" + orderId + "]")
            );
        }
    }

    @PostMapping(value = "/order/setItems", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> setItems(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "userId") String userId,
                                                      @RequestBody OrderAggregateDto dto) {
        try {
            Order order = orderService.findByIdValidateByUser(orderId, userId);

            if (!changesToOrderAllowed(order)) {
                log.warn("setting items to order rejected, orderId: " + orderId);
                throw new CommonApiException(
                        HttpStatus.FORBIDDEN,
                        new Error("rejected-changes-to-order", "rejected changes to order with id [" + orderId + "]")
                );
            }

            setItems(orderId, order, dto);

            return orderAggregateDto(orderId);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("setting order items failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-set-order-items", "failed to set items for order with id [" + orderId + "]")
            );
        }
	}

    @PostMapping(value = "/order/setAccounted", produces = MediaType.APPLICATION_JSON_VALUE)
    public void setOrderAsAccounted(@RequestParam(value = "orderId") String orderId) {
        try {
            log.info("/order/setAccounted - Setting order accounted outside accounting process, orderId: " + orderId);
            orderService.markAsAccounted(orderId);

        } catch (Exception e) {
            log.error("Setting order as accounted failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-set-order-as-accounted", "Failed to set order as accounted")
            );
        }
    }

	private void setItems(String orderId, Order order, OrderAggregateDto dto) {
        if (dto != null && dto.getItems() != null) {
            dto.getItems().stream().forEach(item -> {
                String orderItemId = orderItemService.addItem(
                        orderId,
                        item.getMerchantId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getProductLabel(),
                        item.getProductDescription(),
                        item.getQuantity(),
                        item.getUnit(),
                        item.getRowPriceNet(),
                        item.getRowPriceVat(),
                        item.getRowPriceTotal(),
                        item.getVatPercentage(),
                        item.getPriceNet(),
                        item.getPriceVat(),
                        item.getPriceGross(),
                        item.getOriginalPriceNet(),
                        item.getOriginalPriceVat(),
                        item.getOriginalPriceGross(),
                        item.getPeriodUnit(),
                        item.getPeriodFrequency(),
                        item.getPeriodCount(),
                        item.getBillingStartDate(),
                        item.getStartDate(),
                        item.getInvoicingDate()
                );

                if (item.getMeta() != null) {
                    item.getMeta().stream().forEach(meta -> {
                        meta.setOrderItemId(orderItemId);
                        meta.setOrderId(orderId);
                        orderItemMetaService.addItemMeta(meta);
                    });
                }
            });

            String orderType = orderTypeLogic.decideOrderTypeBasedOnItems(dto.getItems());
            orderService.setType(order, orderType);
        }
    }

    @PostMapping(value = "/order/setTotals", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> setTotals(@RequestParam(value = "orderId") String orderId, @RequestParam(value = "userId") String userId,
                                                       @RequestParam(value = "priceNet") String priceNet, @RequestParam(value = "priceVat") String priceVat,
                                                       @RequestParam(value = "priceTotal") String priceTotal) {
        try {
            Order order = orderService.findByIdValidateByUser(orderId, userId);

            if (!changesToOrderAllowed(order)) {
                log.warn("setting totals to order rejected, orderId: " + orderId);
                throw new CommonApiException(
                        HttpStatus.FORBIDDEN,
                        new Error("rejected-changes-to-order", "rejected changes to order with id [" + orderId + "]")
                );
            }

            orderService.setTotals(order, priceNet, priceVat, priceTotal);
            return orderAggregateDto(orderId);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("setting order totals failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-set-order-totals", "failed to set totals for order with id [" + orderId + "]")
            );
        }
	}

    @PostMapping(value = "/order/createWithItems", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderAggregateDto> createWithItems(@RequestBody OrderAggregateDto orderAggregateDto) {
        try {
            OrderDto orderDto = orderAggregateDto.getOrder();

            Order order = orderService.createByParams(orderDto.getNamespace(), orderDto.getUser(), orderDto.getLastValidPurchaseDateTime());
            String orderId = order.getOrderId();

            String orderType = orderTypeLogic.decideOrderTypeBasedOnItems(orderAggregateDto.getItems());
            orderService.setType(order, orderType);

            CustomerDto customerDto = validateCustomerData(orderDto.getCustomerFirstName(), orderDto.getCustomerLastName(), orderDto.getCustomerEmail(), orderDto.getCustomerPhone());
            orderService.setCustomer(order, customerDto);

            setItems(orderId, order, orderAggregateDto);
            orderService.setTotals(order, orderDto.getPriceNet(), orderDto.getPriceVat(), orderDto.getPriceTotal());

            return orderAggregateDto(orderId);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("creating order with items failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-order-with-items", "failed to create order with items")
            );
        }
    }

    @PostMapping(value = "/order/payment-failed-event", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> paymentFailedEventCallback(@RequestBody PaymentMessage message) {
        try {
            log.debug("order-api received PAYMENT_FAILED event for paymentId: " + message.getPaymentId());

            Order order = orderService.findByIdValidateByUser(message.getOrderId(), message.getUserId());

            // a single order which has subscription id means subscription renewal
            if (order != null && StringUtils.isNotEmpty(order.getSubscriptionId())) {
                log.debug("payment-failed-event callback, subscription renewal payment has failed, subscriptionId: " + order.getSubscriptionId());
                try {
                    JSONObject result = subscriptionService.sendSubscriptionPaymentFailedEmail(order.getSubscriptionId());
                } catch (Exception e) {
                    log.error("Error sending paymentFailedEmail for subscription {}", order.getSubscriptionId(), e);
                }
            } else {
                log.debug("payment-failed-event callback, order payment has failed, orderId: " + order.getOrderId());
                // TODO single order payment failed callback action
            }
            saveHistoryService.savePaymentMessageHistory(message);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("payment paid event callback for order failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("payment-paid-event-callback-for-order-failed", "Payment paid event callback for order failed")
            );
        }

        return ResponseEntity.ok("");
    }

    @PostMapping(value = "/order/payment-paid-event", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> paymentPaidEventCallback(@RequestBody PaymentMessage message) {
        try {
            log.debug("order-api received PAYMENT_PAID event for paymentId: " + message.getPaymentId());
            Order order = orderService.findByIdValidateByUser(message.getOrderId(), message.getUserId());

            // a single order which has subscription id means subscription renewal
            if (order != null && StringUtils.isNotEmpty(order.getSubscriptionId())) {
                subscriptionService.afterRenewalPaymentPaidEventActions(message, order);
            } else {
                log.debug("payment-paid-event callback, orderId: " + order.getOrderId());
                // TODO single order payment paid callback action
            }
            saveHistoryService.savePaymentMessageHistory(message);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("payment paid event callback for order failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("payment-paid-event-callback-for-order-failed", "Payment paid event callback for order failed")
            );
        }

        return ResponseEntity.ok("");
    }

    @PostMapping(value = "/order/payment-paid-webhook", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> paymentPaidWebhook(@RequestBody PaymentMessage message) {
        try {
            // This row validates that message contains authorization to order.
            orderService.findByIdValidateByUser(message.getOrderId(), message.getUserId());
            saveHistoryService.savePaymentMessageHistory(message);
            return restWebHookService.postCallWebHook(message.toCustomerWebHook(), ServiceConfigurationKeys.MERCHANT_PAYMENT_WEBHOOK_URL, message.getNamespace());

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("sending webhook data failed, orderId: " + message.getOrderId(), e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-send-payment-paid-event", "failed to call payment paid webhook for order with id [" + message.getOrderId() + "]")
            );
        }
    }

    @PostMapping(value = "/order/order-cancelled-webhook", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> orderCancelledWebhook(@RequestBody OrderMessage message) {

        try {
            // This row validates that message contains authorization to order.
            orderService.findByIdValidateByUser(message.getOrderId(), message.getUserId());
            saveHistoryService.saveOrderMessageHistory(message);
            return restWebHookService.postCallWebHook(message.toCustomerWebhook(), ServiceConfigurationKeys.MERCHANT_ORDER_WEBHOOK_URL, message.getNamespace());
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("sending webhook data failed, orderId: " + message.getOrderId(), e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-send-order-cancelled-event", "failed to call order cancelled webhook for order with id [" + message.getOrderId() + "]")
            );
        }
    }

    @PostMapping("/order/{orderId}/flowSteps")
    public ResponseEntity<FlowStepDto> saveFlowStepsToOrder(@PathVariable String orderId,
                                                            @RequestBody FlowStepDto flowSteps) {
        try {
            FlowStepDto flowStepDto = flowStepService.saveFlowStepsByOrderId(orderId, flowSteps);
            return ResponseEntity.ok().body(flowStepDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("flow step saving failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-save-flow-steps", "failed to save flow steps for order with id [" + orderId + "]")
            );
        }
    }

    @PostMapping("/order/setPaymentMethod")
    public ResponseEntity<OrderPaymentMethodDto> upsertPaymentMethodToOrder(@RequestBody OrderPaymentMethodDto dto) {
        try {
            OrderPaymentMethodDto savedDto = orderPaymentMethodService.upsertOrderPaymentMethod(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("order payment method setting failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-set-order-payment-method", "failed to set order payment method")
            );
        }
    }

    private ResponseEntity<OrderAggregateDto> orderAggregateDto(String orderId) {
        return orderService.orderAggregateDto(orderId);
    }

    private CustomerDto validateCustomerData(String customerFirstName, String customerLastName, String customerEmail, String customerPhone) {
        CustomerDto customerDto = CustomerDto.builder()
        .customerLastName(customerLastName)
        .customerFirstName(customerFirstName)
        .customerEmail(customerEmail)
        .customerPhone(customerPhone)
        .build();

        commonBeanValidationService.validateInput(customerDto);
        return customerDto;
    }

    private TotalsDto validateOrderTotalsExist(Order order) {
        TotalsDto totalsDto = TotalsDto.builder()
        .priceNet(new BigDecimal(order.getPriceNet()))
        .priceVat(new BigDecimal(order.getPriceVat()))
        .priceTotal(new BigDecimal(order.getPriceTotal()))
        .build();

        commonBeanValidationService.validateInput(totalsDto);
        return totalsDto;
    }

    private boolean changesToOrderAllowed(Order order) {
        boolean changesToOrderAllowed = (order != null && OrderStatus.DRAFT.equals(order.getStatus()));
        log.debug("changesToOrderAllowed order: " + order.getOrderId() + " allowed: " + changesToOrderAllowed);
        return changesToOrderAllowed;
    }

}
