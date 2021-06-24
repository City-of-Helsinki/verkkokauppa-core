package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import org.junit.Test;

import javax.validation.ConstraintValidator;
import javax.validation.Valid;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class OrderTransformerTests {
    private OrderTransformer orderTransformer = new OrderTransformer();

    private List<String> excludedFields = new ArrayList<>();


    /**
     * This test is just a reminder.
     * Check that all the necessary fields are included in data generation.
     * Exclude the ones that are not needed.
     * @throws IllegalAccessException
     */
    @Test
    public void should_NotHaveNullFieldsDto() throws IllegalAccessException {
        final OrderDto dto = generateDummyOrderDto();
        final boolean hasNullFields = TestUtils.hasNullFields(dto, excludedFields);

        assertFalse(hasNullFields);
    }


    /**
     * This test is just a reminder.
     * Check that all the necessary fields are included in data generation.
     * Exclude the ones that are not needed.
     * @throws IllegalAccessException
     */
    @Test
    public void should_NotHaveNullFieldsEntity() throws IllegalAccessException{
        final Order entity = generateDummyOrder();
        final boolean hasNullFields = TestUtils.hasNullFields(entity, excludedFields);
        assertFalse(hasNullFields);
    }

    @Test
    public void should_transformDtoToEntity() {
        final OrderDto dto = generateDummyOrderDto();
        final Order entity = orderTransformer.transformToEntity(dto);

        assertNotNull(entity);
    }

    @Test
    public void should_TransformEntityToDto() {
        final Order entity = generateDummyOrder();
        final OrderDto dto = orderTransformer.transformToDto(entity);

        assertNotNull(dto);
    }


    @Test
    public void should_GiveException_WhenEmailIsWrongAndValidated() {
        ConstraintValidator<>
        OrderDto dto = generateDummyOrderDto();
        dto.setCustomerEmail("not an email address!");
        

    }

    private Order generateDummyOrder() {
        Order order = new Order();
        order.setOrderId("1");
        order.setCreatedAt("today");
        order.setUser("dummy_user");
        order.setNamespace("dummy_namespace");
        order.setStatus("dummy_status");
        order.setCustomerFirstName("dummy_firstname");
        order.setCustomerLastName("dummy_lastname");
        order.setCustomerEmail("dummy@example.com");
        order.setType("dummy_type");

        return order;
    }

    private OrderDto generateDummyOrderDto() {
        OrderDto dto = OrderDto.builder()
                .orderId("1")
                .createdAt("today")
                .customerEmail("dummy_email")
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
