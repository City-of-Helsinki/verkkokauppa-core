package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class OrderTransformerTests extends DummyData {
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
        excludedFields.add("subscriptionId");
        excludedFields.add("priceNet");
        excludedFields.add("priceVat");
        excludedFields.add("priceTotal");
        excludedFields.add("accounted");
        excludedFields.add("customerPhone");
        excludedFields.add("invoice");
        excludedFields.add("incrementId");
        excludedFields.add("lastValidPurchaseDateTime");
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
        excludedFields.add("subscriptionId");
        excludedFields.add("priceNet");
        excludedFields.add("priceVat");
        excludedFields.add("priceTotal");
        excludedFields.add("accounted");
        excludedFields.add("customerPhone");
        excludedFields.add("startDate");
        excludedFields.add("endDate");
        excludedFields.add("invoice");
        excludedFields.add("incrementId");
        excludedFields.add("lastValidPurchaseDateTime");
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

 }
