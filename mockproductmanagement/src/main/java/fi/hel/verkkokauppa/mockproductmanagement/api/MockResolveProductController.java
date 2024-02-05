package fi.hel.verkkokauppa.mockproductmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.response.OrderRightOfPurchaseResponse;
import fi.hel.verkkokauppa.mockproductmanagement.api.subscription.request.MockResolveProductRequest;
import fi.hel.verkkokauppa.mockproductmanagement.api.subscription.response.MockResolveProductResultDto;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@RestController
public class MockResolveProductController {
    private Logger log = LoggerFactory.getLogger(MockResolveProductController.class);

    @Autowired
    private ObjectMapper objectMapper;
    @PostMapping("mock/*/subscription/product")
    public ResponseEntity<MockResolveProductResultDto> getMockedResolveProduct(@RequestBody MockResolveProductRequest data) {
        log.info("getMockedResolveProduct received request: " + data.toString());
        try {
            JSONObject resolveProductResultDto = new JSONObject();
            resolveProductResultDto.put("subscriptionId","dummyProductId");
            resolveProductResultDto.put("userId","userId");
            resolveProductResultDto.put("productId","b86337e8-68a0-3599-a18b-754ffae53f5a");
            resolveProductResultDto.put("productName","newProductName");
            resolveProductResultDto.put("productLabel","newProductLabel");
            resolveProductResultDto.put("productDescription","newProductDescription");

            if( !data.getOrderItem().getMeta().isEmpty() )
            {
                JSONObject ResolveProductMetaDto1 = new JSONObject();
                ResolveProductMetaDto1.put("key", "key1");
                ResolveProductMetaDto1.put("value", "Value1");
                ResolveProductMetaDto1.put("label", "label1");
                ResolveProductMetaDto1.put("visibleInCheckout", "true");
                ResolveProductMetaDto1.put("ordinal", "2");
                JSONObject ResolveProductMetaDto2 = new JSONObject();
                ResolveProductMetaDto2.put("key", "key2");
                ResolveProductMetaDto2.put("value", "Value2");
                ResolveProductMetaDto2.put("label", "label2");
                ResolveProductMetaDto2.put("visibleInCheckout", "true");
                ResolveProductMetaDto2.put("ordinal", "1");
                Collection<JSONObject> orderItemMetas = new ArrayList<JSONObject>();
                orderItemMetas.add(ResolveProductMetaDto1);
                orderItemMetas.add(ResolveProductMetaDto2);
                resolveProductResultDto.put("orderItemMetas",orderItemMetas);
            }

            return ResponseEntity.ok().body(objectMapper.readValue(Objects.requireNonNull(resolveProductResultDto.toString()), MockResolveProductResultDto.class));
        } catch (Exception e) {
            log.error("Resolve product failed, data: " + data, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
