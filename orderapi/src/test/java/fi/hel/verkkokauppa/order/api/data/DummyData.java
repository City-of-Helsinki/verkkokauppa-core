package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;

import java.util.ArrayList;
import java.util.List;

public abstract class DummyData {
    public Order generateDummyOrder() {
        Order order = new Order();
        order.setOrderId("1");
        order.setCreatedAt("today");
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
                .createdAt("today")
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

    public List<OrderAccounting> generateDummyOrderAccountingList() {
        OrderAccounting orderAccounting1 = new OrderAccounting();
        orderAccounting1.setOrderId("1");
        orderAccounting1.setCreatedAt("2021-09-01");
        OrderAccounting orderAccounting2 = new OrderAccounting();
        orderAccounting2.setOrderId("2");
        orderAccounting2.setCreatedAt("2021-09-01");
        OrderAccounting orderAccounting3 = new OrderAccounting();
        orderAccounting3.setOrderId("3");
        orderAccounting3.setCreatedAt("2021-09-02");

        List<OrderAccounting> orderAccountings = new ArrayList<>();
        orderAccountings.add(orderAccounting1);
        orderAccountings.add(orderAccounting2);
        orderAccountings.add(orderAccounting3);

        return orderAccountings;
    }
}
