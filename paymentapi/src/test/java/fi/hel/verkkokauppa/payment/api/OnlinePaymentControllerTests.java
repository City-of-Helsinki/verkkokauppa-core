package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.payment.api.OnlinePaymentController;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.logic.PaymentReturnValidator;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "payment_encryption_key=payment_encryption_key",
})
public class OnlinePaymentControllerTests {

    @Autowired
    private OnlinePaymentController onlinePaymentController;
    @Autowired
    private OnlinePaymentService onlinePaymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    Environment env;

    //This test is ignored because uses pure elastic search and not mocks to make testing easier.
    @Test
    public void testUpdatePaymentStatus() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method privateMethod = OnlinePaymentController.class.getDeclaredMethod("updatePaymentStatus", String.class, PaymentReturnDto.class);
        privateMethod.setAccessible(true);

        privateMethod.invoke(onlinePaymentController,"test",new PaymentReturnDto(true,true,true));
//        System.out.println("returnValue = " + returnValue);
    }

    @Test
    public void name() {
//        onlinePaymentService.getPaymentRequestData()
        OrderWrapper wrapper = new OrderWrapper();
        try {
            wrapper.setOrder(objectMapper.readValue("{\n" +
                    "      \"_class\": \"fi.hel.verkkokauppa.order.model.Order\",\n" +
                    "      \"orderId\": \"66c05194-92bf-3153-ab09-bf6820fa3f1b\",\n" +
                    "      \"namespace\": \"venepaikat\",\n" +
                    "      \"user\": \"dummy_user\",\n" +
                    "      \"createdAt\": \"2021-10-25T17:18:34.0864654\",\n" +
                    "      \"status\": \"draft\",\n" +
                    "      \"type\": \"subscription\",\n" +
                    "      \"customerFirstName\": \"dummy_firstname\",\n" +
                    "      \"customerLastName\": \"dummy_lastname\",\n" +
                    "      \"customerEmail\": \"ec06288a-6407-42a2-ad5e-1b4bbac541ba@ambientia.fi\"\n" +
                    "    }",OrderDto.class));
            wrapper.setItems(new ArrayList<OrderItemDto>(
                    objectMapper.readValue("{\n" +
                            "      \"_class\": \"fi.hel.verkkokauppa.order.model.OrderItem\",\n" +
                            "      \"orderItemId\": \"e59a291d-59a3-4973-b02b-c23e1af1a197\",\n" +
                            "      \"orderId\": \"66c05194-92bf-3153-ab09-bf6820fa3f1b\",\n" +
                            "      \"productId\": \"productId\",\n" +
                            "      \"productName\": \"productName\",\n" +
                            "      \"quantity\": 1,\n" +
                            "      \"unit\": \"unit\",\n" +
                            "      \"rowPriceNet\": \"100\",\n" +
                            "      \"rowPriceVat\": \"100\",\n" +
                            "      \"rowPriceTotal\": \"100\",\n" +
                            "      \"vatPercentage\": \"0\",\n" +
                            "      \"priceGross\": \"124\",\n" +
                            "      \"type\": \"subscription\",\n" +
                            "      \"startDate\": \"2021-10-25T17:18:34.015Z\",\n" +
                            "      \"billingStartDate\": \"2021-10-25T17:18:34.015Z\",\n" +
                            "      \"periodUnit\": \"daily\",\n" +
                            "      \"periodFrequency\": 1,\n" +
                            "      \"periodCount\": 2\n" +
                            "    }", OrderItemDto.class)
            ));

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}