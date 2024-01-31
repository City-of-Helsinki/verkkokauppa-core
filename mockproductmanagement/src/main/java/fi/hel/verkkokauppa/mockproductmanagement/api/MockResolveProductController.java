package fi.hel.verkkokauppa.mockproductmanagement.api;

import fi.hel.verkkokauppa.common.response.OrderRightOfPurchaseResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;

@RestController
public class MockResolveProductController {
    private Logger log = LoggerFactory.getLogger(MockResolveProductController.class);

    @PostMapping("mock/*/subscription/product")
    public ResponseEntity<JSONObject> getOrderRightOfPurchase(@RequestBody String data) {
        try {
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
            JSONObject ResolveProductResultDto = new JSONObject();
            ResolveProductResultDto.put("subscriptionId","dummyProductId");
            ResolveProductResultDto.put("userId","userId");
            ResolveProductResultDto.put("productId","newProductId");
            ResolveProductResultDto.put("productName","newProductName");
            ResolveProductResultDto.put("productLabel","newProductLabel");
            ResolveProductResultDto.put("productDescription","newProductDescription");
            ResolveProductResultDto.put("orderItemMetas",orderItemMetas);

            ResponseEntity<JSONObject> resolveProductResponse = new ResponseEntity<>( ResolveProductResultDto, HttpStatus.OK);

            return resolveProductResponse;
        } catch (Exception e) {
            log.error("Resolve product failed, data: " + data, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
