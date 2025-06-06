package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.accounting.ProductAccountingDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;

public abstract class DummyData {
    public Order generateDummyOrder() {
        Order order = new Order();
        order.setOrderId("1");
        order.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        order.setUser("dummy_user");
        order.setNamespace("dummy_namespace");
        order.setStatus("dummy_status");
        order.setCustomerFirstName("dummy_firstname");
        order.setCustomerLastName("dummy_lastname");
        order.setCustomerEmail("dummy@dummymail.com");
        order.setType("dummy_type");

        return order;
    }

    public Subscription generateDummySubscription(Order order) {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId("1");
        subscription.setOrderId(order.getOrderId());
        subscription.setOrderItemId("1");
        subscription.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        subscription.setUser("dummy_user");
        subscription.setNamespace("dummy_namespace");
        subscription.setStatus("dummy_status");
        subscription.setCustomerFirstName("dummy_firstname");
        subscription.setCustomerLastName("dummy_lastname");
        subscription.setCustomerEmail("dummy@dummymail.com");

        return subscription;
    }

    public OrderDto generateDummyOrderDto() {
        OrderDto dto = OrderDto.builder()
                .orderId("1")
                .createdAt(DateTimeUtil.getFormattedDateTime())
                .customerEmail("dummy_email@example.com")
                .customerFirstName("dummy_firstname")
                .customerLastName("dummy_lastname")
                .namespace("dummy_namespace")
                .status("dummy_status")
                .type("dummy_type")
                .user("dummy_user")
                .build();

        return dto;
    }

    public ArrayList<Order> generateDummyOrderList() {
        Order order1 = generateDummyOrder();
        Order order2 = generateDummyOrder();
        Order order3 = generateDummyOrder();
        order2.setOrderId("2");
        order3.setOrderId("3");

        ArrayList<Order> orders = new ArrayList<>();
        orders.add(order1);
        orders.add(order2);
        orders.add(order3);

        return orders;
    }

    public OrderItem generateDummyOrderItem(Order order){
        return generateDummyOrderItem(order, "9876", "8a8674ed-1ae2-3ca9-a93c-036478b2a032");
    }

    public OrderItem generateDummyOrderItem(Order order, String merchantId, String productId) {
        String orderItemId = UUIDGenerator.generateType4UUID().toString();
        //String orderId, String productId, String productName, Integer quantity, String unit, String rowPriceNet, String rowPriceVat, String rowPriceTotal, String vatPercentage, String priceNet, String priceVat, String priceGross
        return new OrderItem(
                orderItemId,
                order.getOrderId(),
                merchantId,
                productId,
                "productName",
                "productLabel",
                "productDescription",
                1,
                "unit",
                "100",
                "100",
                "100",
                "0",
                order.getPriceNet(),
                order.getPriceVat(),
                "100",
                order.getPriceNet(),
                order.getPriceVat(),
                "100",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public OrderItem generateDummyFreeOrderItem(Order order) {
        String orderItemId = UUIDGenerator.generateType4UUID().toString();
        //String orderId, String productId, String productName, Integer quantity, String unit, String rowPriceNet, String rowPriceVat, String rowPriceTotal, String vatPercentage, String priceNet, String priceVat, String priceGross
        return new OrderItem(
                orderItemId,
                order.getOrderId(),
                "9876",
                "8a8674ed-1ae2-3ca9-a93c-036478b2a032",
                "productName",
                "productLabel",
                "productDescription",
                1,
                "unit",
                "0.0",
                "0.0",
                "0.0",
                "0",
                "0.0",
                "0.0",
                "0.0",
                "0.0",
                "0.0",
                "0.0",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public ProductAccountingDto createDummyProductAccountingDto(String productId, String prefix){
        ProductAccountingDto dto = new ProductAccountingDto();
        dto.setInternalOrder(prefix + "internalOrder");
        dto.setMainLedgerAccount(prefix + "mainLedgerAccount");
        dto.setProject(prefix + "project");
        dto.setOperationArea(prefix + "operationArea");
        dto.setVatCode(prefix + "vatCode");
        dto.setProfitCenter(prefix + "profitCenter");
        dto.setCompanyCode(prefix + "companyCode");
        dto.setProductId(productId);
        dto.setBalanceProfitCenter(prefix + "balanceProfitCenter");

        return dto;
    }

    public OrderItemMeta generateDummyOrderItemMeta(OrderItem orderItem,String ordinal) {
        String orderItemMetaId = UUIDGenerator.generateType4UUID().toString();
        //String orderId, String productId, String productName, Integer quantity, String unit, String rowPriceNet, String rowPriceVat, String rowPriceTotal, String vatPercentage, String priceNet, String priceVat, String priceGross
        return new OrderItemMeta(
                orderItemMetaId,
                orderItem.getOrderItemId(),
                orderItem.getOrderId(),
                "meta key",
                "meta value",
                "meta label",
                "true",
                ordinal
        );
    }

    public List<OrderItem> generateDummyOrderItemList(Order order,int itemCount){
        return generateDummyOrderItemList(order, itemCount, "9876", "8a8674ed-1ae2-3ca9-a93c-036478b2a032");
    }

    public List<OrderItem> generateDummyOrderItemList(Order order,int itemCount, String merchantId, String productId) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            orderItems.add(generateDummyOrderItem(order, merchantId, productId));
        }

        return orderItems;
    }


    public List<OrderItemMeta> generateDummyOrderItemMetaList(List<OrderItem> orderItems) {
        List<OrderItemMeta> orderItemMetas = new ArrayList<>();
        for (int i = 0; i < orderItems.size(); i++) {
            orderItemMetas.add(generateDummyOrderItemMeta(orderItems.get(i),Integer.toString(i)));
        }

        return orderItemMetas;
    }

    public List<OrderAccounting> generateDummyOrderAccountingList() {
        OrderAccounting orderAccounting1 = new OrderAccounting();
        orderAccounting1.setOrderId("1");
        orderAccounting1.setCreatedAt(DateTimeUtil.fromFormattedDateString("2021-09-01 20:30:00"));
        OrderAccounting orderAccounting2 = new OrderAccounting();
        orderAccounting2.setOrderId("2");
        orderAccounting2.setCreatedAt(DateTimeUtil.fromFormattedDateString("2021-09-01 23:00:00"));
        OrderAccounting orderAccounting3 = new OrderAccounting();
        orderAccounting3.setOrderId("3");
        orderAccounting3.setCreatedAt(DateTimeUtil.fromFormattedDateString("2021-09-02 16:00:00"));

        List<OrderAccounting> orderAccountings = new ArrayList<>();
        orderAccountings.add(orderAccounting1);
        orderAccountings.add(orderAccounting2);
        orderAccountings.add(orderAccounting3);

        return orderAccountings;
    }

    public Refund generateDummyRefund(String orderId) {
        Refund refund = new Refund();
        refund.setRefundId("1");
        refund.setOrderId(orderId);
        refund.setCreatedAt(DateTimeUtil.getFormattedDateTime());
        refund.setUser("dummy_user");
        refund.setNamespace("dummy_namespace");
        refund.setStatus("dummy_status");
        refund.setCustomerFirstName("dummy_firstname");
        refund.setCustomerLastName("dummy_lastname");
        refund.setCustomerEmail("dummy@dummymail.com");
        refund.setRefundReason("dummy_reason");

        return refund;
    }

    public RefundItemDto generateDummyRefundItemDto(Refund refund) {
        String refundItemId = UUIDGenerator.generateType4UUID().toString();
        String orderItemId = UUIDGenerator.generateType4UUID().toString();

        return new RefundItemDto(
                refundItemId,
                refund.getRefundId(),
                orderItemId,
                refund.getOrderId(),
                "9876",
                "8a8674ed-1ae2-3ca9-a93c-036478b2a032",
                "productName",
                "productLabel",
                "productDescription",
                "unit",
                1,
                "70",
                "30",
                "100",
                "30",
                refund.getPriceNet(),
                refund.getPriceVat(),
                "100",
                refund.getPriceNet(),
                refund.getPriceVat(),
                "100"
        );
    }

    public RefundItem generateDummyRefundItem(Refund refund) {

        return new RefundItem(
                refund.getRefundId(),
                generateDummyRefundItemDto(refund)
        );
    }

    public List<RefundItem> generateDummyRefundItemList(Refund refund, String orderId, int itemCount) {
        List<RefundItem> refundItems = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            refundItems.add(generateDummyRefundItem(refund));
        }

        return refundItems;
    }

    public RefundDto generateDummyRefundDto(String orderId) {
        RefundDto dto = RefundDto.builder()
                .orderId(orderId)
                .createdAt(DateTimeUtil.getFormattedDateTime().toString())
                .customerEmail("dummy_email@example.com")
                .customerFirstName("dummy_firstname")
                .customerLastName("dummy_lastname")
                .namespace("dummy_namespace")
                .status("dummy_status")
                .refundReason("dummy_reason")
                .user("dummy_user")
                .build();

        return dto;
    }


}
