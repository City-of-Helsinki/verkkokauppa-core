package fi.hel.verkkokauppa.order.api.data.dto;

import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Iterator;
import java.util.Set;

public class OrderDtoValidationTests extends DummyData {

    private Validator validator;

    @Before
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void should_BeValidOrderDto() {
        // minimum valid dto
        OrderDto dto = new OrderDto();
        dto.setCustomerFirstName("junit_test_firstname");
        dto.setCustomerLastName("junit_test_lastname");
        dto.setCustomerEmail("junit_test@junit_test.email");

        Set<ConstraintViolation<OrderDto>> constraintViolations =  validator.validate(dto);

        // there should be no raised violations
        assertTrue(constraintViolations.isEmpty());
    }

    @Test
    public void should_GiveFirstnameRequiredMessage_IfLastnameIsBlank() {
        OrderDto dto = generateDummyOrderDto();
        dto.setCustomerLastName("");
        Set<ConstraintViolation<OrderDto>> constraintViolations =  validator.validate(dto);

        final String message = extractMessage(constraintViolations);
        //todo: remove hardcoded messages
        assertEquals(message, "firstname required");
    }

    @Test
    public void should_GiveFirstnameRequiredMessage_IfLastnameIsNull() {
        OrderDto dto = generateDummyOrderDto();
        dto.setCustomerLastName(null);
        Set<ConstraintViolation<OrderDto>> constraintViolations =  validator.validate(dto);

        final String message = extractMessage(constraintViolations);
        //todo: remove hardcoded messages
        assertEquals(message, "firstname required");
    }

    @Test
    public void should_GiveFirstnameRequiredMessage_IfFirstnameIsNull() {
        OrderDto dto = generateDummyOrderDto();
        dto.setCustomerFirstName(null);
        Set<ConstraintViolation<OrderDto>> constraintViolations =  validator.validate(dto);

        final String message = extractMessage(constraintViolations);
        //todo: remove hardcoded messages
        assertEquals(message, "firstname required");
    }

    @Test
    public void should_GiveFirstnameRequiredMessage_IfFirstnameIsBlank() {
        OrderDto dto = generateDummyOrderDto();
        dto.setCustomerFirstName("");
        Set<ConstraintViolation<OrderDto>> constraintViolations =  validator.validate(dto);

        final String message = extractMessage(constraintViolations);
        //todo: remove hardcoded messages
        assertEquals(message, "firstname required");
    }

    @Test
    public void should_GiveInvalidEmailAddressViolationMessage() {
        OrderDto dto = generateDummyOrderDto();
        dto.setCustomerEmail("not_valid_email_address");
        Set<ConstraintViolation<OrderDto>> constraintViolations =  validator.validate(dto);

        final String message = extractMessage(constraintViolations);
        //todo: remove hardcoded messages
        assertEquals(message, "email must be in correct format");
    }


    private String extractMessage(Set<ConstraintViolation<OrderDto>> constraintViolations) {
        String message = "";
        for (Iterator<ConstraintViolation<OrderDto>> it = constraintViolations.iterator(); it.hasNext(); ) {
            ConstraintViolation<OrderDto> violation = it.next();
            message = violation.getMessage();
        }
        return message;
    }
}
