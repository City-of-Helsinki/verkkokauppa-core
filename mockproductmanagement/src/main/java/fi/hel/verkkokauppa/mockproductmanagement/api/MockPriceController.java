package fi.hel.verkkokauppa.mockproductmanagement.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MockPriceController {

    private Map<String, MockPrice> mockPricesAP = new HashMap<String, MockPrice>();
    private Map<String, MockPrice> mockPricesTV = new HashMap<String, MockPrice>();

    {
        mockPricesAP.put("1234", new MockPrice("1", "asukaspysakointi", "1234", "100", "24", "24", "124"));
        mockPricesAP.put("12345", new MockPrice("2", "asukaspysakointi", "12345", "200", "24", "48", "248"));
        mockPricesAP.put("123456", new MockPrice("3", "asukaspysakointi", "123456", "300", "24", "72", "372"));

        mockPricesTV.put("1234", new MockPrice("11", "tilavaraus", "1234", "1000", "24", "240", "1240"));
        mockPricesTV.put("12345", new MockPrice("22", "tilavaraus", "12345", "2000", "24", "480", "2480"));
        mockPricesTV.put("123456", new MockPrice("33", "tilavaraus", "123456", "3000", "24", "720", "3720"));
    }

    @GetMapping("/mockprice/asukaspysakointi/get")
    public MockPrice getMockPriceAP(@RequestParam(value = "productId") String productId) {
        return mockPricesAP.get(productId);
    }

    @GetMapping("/mockprice/tilavaraus/get")
    public MockPrice getMockPriceTV(@RequestParam(value = "productId") String productId) {
        return mockPricesTV.get(productId);
    }

}
