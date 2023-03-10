package fi.hel.verkkokauppa.payment.testing;

import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.dto.configuration.MerchantDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunIfProfile(profile = "local")
@RunWith(SpringRunner.class)
@SpringBootTest
public abstract class BaseFunctionalTest {

    @Autowired
    protected CommonServiceConfigurationClient commonServiceConfigurationClient;

    protected String getFirstMerchantIdFromNamespace(String namespace) {
        List<MerchantDto> merchants = commonServiceConfigurationClient.getMerchantsForNamespace(namespace);
        if (merchants.size() > 0) {
            return merchants.get(0).getMerchantId();
        }
        return null;
    }

    protected OrderWrapper createDummyOrderWrapper() {
        OrderDto orderDto = new OrderDto();
        String orderId = UUID.randomUUID().toString();
        orderDto.setOrderId(orderId);
        orderDto.setNamespace("venepaikat");
        orderDto.setUser("dummy_user");
        orderDto.setCreatedAt("");
        orderDto.setStatus("confirmed");
        orderDto.setType("order");
        orderDto.setCustomerFirstName("Martin");
        orderDto.setCustomerLastName("Leh");
        orderDto.setCustomerEmail("testi@ambientia.fi");
        orderDto.setPriceNet("1234");
        orderDto.setPriceVat("0");
        // Sets total price to be 1 eur
        orderDto.setPriceTotal("1234");

        OrderWrapper order = new OrderWrapper();
        order.setOrder(orderDto);

        List<OrderItemDto> items = new ArrayList<>();

        OrderItemDto orderItem = new OrderItemDto();

        String orderItemId = UUID.randomUUID().toString();
        orderItem.setOrderItemId(orderItemId);
        orderItem.setPriceGross(BigDecimal.valueOf(1234));
        orderItem.setQuantity(1);
        orderItem.setVatPercentage("24");
        orderItem.setProductId("test-product-id");
        orderItem.setProductName("productNäme");
        orderItem.setOrderId(orderId);

        items.add(orderItem);

        order.setItems(items);
        return order;
    }
}
