package fi.hel.verkkokauppa.backend.api;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.mockproductmanagement.api.MockAvailability;
import fi.hel.verkkokauppa.mockproductmanagement.api.MockPrice;
import fi.hel.verkkokauppa.mockproductmanagement.api.MockProduct;
import fi.hel.verkkokauppa.mockproductmanagement.api.MockRightOfPurchase;


@RestController
public class VenepaikatController {
    private Logger log = LoggerFactory.getLogger(VenepaikatController.class);

    private Map<String, MockProduct> products = new HashMap<String, MockProduct>();
    private Map<String, MockPrice> prices = new HashMap<String, MockPrice>();
    private Map<String, MockAvailability> availabilities = new HashMap<String, MockAvailability>();
    private Map<String, MockRightOfPurchase> purchaseRights = new HashMap<String, MockRightOfPurchase>();

    {
        products.put("1234", new MockProduct("1234", "Traileripaikka", "Helsingin kaupunki, Venepaikat, Traileripaikka", "venepaikat"));
        prices.put("1234", new MockPrice("price_generic_1234", "venepaikat", "1234", "64,52", "24", "15,48", "80"));
        availabilities.put("1234", new MockAvailability("availability_generic_1234", "venepaikat", "1234", true));
        purchaseRights.put("1234", new MockRightOfPurchase("rightofpurchase_generic_1234", "venepaikat", "1234", true));
    }

    @GetMapping("/backend/venepaikat/product")
    public ResponseEntity<MockProduct> getProduct(@RequestParam(value = "productId") String productId) {
        try {
            MockProduct product = products.get(productId);

            if (product == null)
                return ResponseEntity.notFound().build();

            return ResponseEntity.ok().body(product);
        } catch (Exception e) {
            log.error("getting product failed, productId: " + productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/backend/venepaikat/price")
    public ResponseEntity<MockPrice> getPrice(@RequestParam(value = "productId") String productId) {
        try {
            MockPrice product = prices.get(productId);

            if (product == null)
                return ResponseEntity.notFound().build();

            return ResponseEntity.ok().body(product);
        } catch (Exception e) {
            log.error("getting product price failed, productId: " + productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/backend/venepaikat/availability")
    public ResponseEntity<MockAvailability> getAvailability(@RequestParam(value = "productId") String productId) {
        try {
            MockAvailability product = availabilities.get(productId);

            if (product == null)
                return ResponseEntity.notFound().build();

            return ResponseEntity.ok().body(product);
        } catch (Exception e) {
            log.error("getting product availability failed, productId: " + productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/backend/venepaikat/right-of-purchase")
    public ResponseEntity<MockRightOfPurchase> getRightOfPurchase(@RequestParam(value = "productId") String productId) {
        try {
            MockRightOfPurchase product = purchaseRights.get(productId);

            if (product == null)
                return ResponseEntity.notFound().build();

            return ResponseEntity.ok().body(product);
        } catch (Exception e) {
            log.error("getting product right of purchase failed, productId: " + productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
