package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class DummyData {
    public Order generateDummyOrder() {
        Order order = new Order();
        order.setOrderId("1");
        order.setCreatedAt(DateTimeUtil.getDateTime());
        order.setUser("dummy_user");
        order.setNamespace("dummy_namespace");
        order.setStatus("dummy_status");
        order.setCustomerFirstName("dummy_firstname");
        order.setCustomerLastName("dummy_lastname");
        order.setCustomerEmail("dummy@dummymail.com");
        order.setType("dummy_type");

        return order;
    }

    public OrderDto generateDummyOrderDto() {
        OrderDto dto = OrderDto.builder()
                .orderId("1")
                .createdAt(DateTimeUtil.getDateTime())
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

    public OrderItem generateDummyOrderItem(Order order) {
        String orderItemId = UUIDGenerator.generateType4UUID().toString();
        //String orderId, String productId, String productName, Integer quantity, String unit, String rowPriceNet, String rowPriceVat, String rowPriceTotal, String vatPercentage, String priceNet, String priceVat, String priceGross
        return new OrderItem(
                orderItemId,
                order.getOrderId(),
                "productId",
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
                null
        );
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

    public List<OrderItem> generateDummyOrderItemList(Order order,int itemCount) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            orderItems.add(generateDummyOrderItem(order));
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
        orderAccounting1.setCreatedAt(DateTimeUtil.fromFormattedDateString("2021-09-01"));
        OrderAccounting orderAccounting2 = new OrderAccounting();
        orderAccounting2.setOrderId("2");
        orderAccounting2.setCreatedAt(DateTimeUtil.fromFormattedDateString("2021-09-01"));
        OrderAccounting orderAccounting3 = new OrderAccounting();
        orderAccounting3.setOrderId("3");
        orderAccounting3.setCreatedAt(DateTimeUtil.fromFormattedDateString("2021-09-02"));

        List<OrderAccounting> orderAccountings = new ArrayList<>();
        orderAccountings.add(orderAccounting1);
        orderAccountings.add(orderAccounting2);
        orderAccountings.add(orderAccounting3);

        return orderAccountings;
    }
}
