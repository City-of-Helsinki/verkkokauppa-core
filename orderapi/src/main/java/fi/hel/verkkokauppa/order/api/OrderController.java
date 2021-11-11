package fi.hel.verkkokauppa.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.order.api.data.CustomerDto;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.TotalsDto;
import fi.hel.verkkokauppa.order.logic.OrderTypeLogic;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderStatus;
import fi.hel.verkkokauppa.order.service.CommonBeanValidationService;
import fi.hel.verkkokauppa.order.service.order.OrderItemMetaService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import fi.hel.verkkokauppa.order.service.order.OrderService;
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

@RestController
public class OrderController {

	private Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemMetaService orderItemMetaService;

	@Autowired
	private OrderTypeLogic orderTypeLogic;

	@Autowired
    private CommonBeanValidationService commonBeanValidationService;

	@Autowired
    private RestServiceClient restServiceClient;

	@Autowired
    private CommonServiceConfigurationClient configurations;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping(value = "/order/create", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderAggregateDto> createOrder(@RequestParam(value = "namespace") String namespace,
                                                         @RequestParam(value = "user") String user) {
        try {
            Order order = orderService.createByParams(namespace, user);
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

	private void setItems(String orderId, Order order, OrderAggregateDto dto) {
        if (dto != null && dto.getItems() != null) {
            dto.getItems().stream().forEach(item -> {
                String orderItemId = orderItemService.addItem(
                        orderId,
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnit(),
                        item.getRowPriceNet(),
                        item.getRowPriceVat(),
                        item.getRowPriceTotal(),
                        item.getVatPercentage(),
                        item.getPriceNet(),
                        item.getPriceVat(),
                        item.getPriceGross(),
                        item.getPeriodUnit(),
                        item.getPeriodFrequency(),
                        item.getPeriodCount(),
                        item.getBillingStartDate(),
                        item.getStartDate()
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
            Order order = orderService.createByParams(orderDto.getNamespace(), orderDto.getUser());
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

    @PostMapping(value = "/order/payment-paid-webhook", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> paymentPaidWebhook(@RequestBody PaymentMessage message) {

        String webhookUrl = configurations.getRestrictedServiceConfigurationValue(message.getNamespace(),"MERCHANT_PAYMENT_WEBHOOK_URL");

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // This row validates that message contains authorization to order.
            orderService.findByIdValidateByUser(message.getOrderId(), message.getUserId());

            //format payload, message to json string conversion
            String body = objectMapper.writeValueAsString(message);

            restServiceClient.makeVoidPostCall(webhookUrl, body);
            return ResponseEntity.ok().build();
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("sending webhook data failed, orderId: " + message.getOrderId(), e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-send-payment-paid-event", "failed to set totals for order with id [" + message.getOrderId() + "]")
            );
        }

    }

    private boolean changesToOrderAllowed(Order order) {
        boolean changesToOrderAllowed = (order != null && OrderStatus.DRAFT.equals(order.getStatus()));
        log.debug("changesToOrderAllowed order: " + order.getOrderId() + " allowed: " + changesToOrderAllowed);
        return changesToOrderAllowed;
    }

    
}
