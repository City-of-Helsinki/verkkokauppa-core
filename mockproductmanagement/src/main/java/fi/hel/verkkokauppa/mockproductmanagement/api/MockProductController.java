package fi.hel.verkkokauppa.mockproductmanagement.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MockProductController {

    private Map<String, MockProduct> mockProductsAP = new HashMap<String, MockProduct>();
    private Map<String, MockProduct> mockProductsTV = new HashMap<String, MockProduct>();

    {
        mockProductsAP.put("1234", new MockProduct("1234", "asukaspysakointi tuote 1", "kuvaus", "asukaspysakointi"));
        mockProductsAP.put("12345", new MockProduct("12345", "asukaspysakointi tuote 2", "kuvaus", "asukaspysakointi"));
        mockProductsAP.put("123456", new MockProduct("123456", "asukaspysakointi tuote 3", "kuvaus", "asukaspysakointi"));

        mockProductsTV.put("1234", new MockProduct("1234", "tilavaraus tuote 1", "kuvaus", "tilavaraus"));
        mockProductsTV.put("12345", new MockProduct("12345", "tilavaraus tuote 2", "kuvaus", "tilavaraus"));
        mockProductsTV.put("123456", new MockProduct("123456", "tilavaraus tuote 3", "kuvaus", "tilavaraus"));
    }

    @GetMapping("/mockproductmanagement/asukaspysakointi/get")
    public MockProduct getMockProductAP(@RequestParam(value = "productId") String productId) {
        return mockProductsAP.get(productId);
    }

    @GetMapping("/mockproductmanagement/tilavaraus/get")
    public MockProduct getMockProductTV(@RequestParam(value = "productId") String productId) {
        return mockProductsTV.get(productId);
    }

}
