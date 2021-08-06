package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.order.model.Order;

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
}
