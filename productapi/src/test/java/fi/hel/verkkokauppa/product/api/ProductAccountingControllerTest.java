package fi.hel.verkkokauppa.product.api;

import fi.hel.verkkokauppa.product.dto.ProductAccountingDto;
import fi.hel.verkkokauppa.product.model.ProductAccounting;
import fi.hel.verkkokauppa.product.repository.ProductAccountingRepository;
import fi.hel.verkkokauppa.product.service.ProductAccountingService;
import fi.hel.verkkokauppa.product.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunIfProfile(profile = "local")
@SpringBootTest
@Slf4j
public class ProductAccountingControllerTest {
    @Autowired
    private ProductAccountingController productAccountingController;

    @Autowired
    private ProductAccountingService productAccountingService;

    @Autowired
    private ProductAccountingRepository productAccountingRepository;

    static private ArrayList<String> productAccountingsToDelete = new ArrayList<>();

    @AfterEach
    public void tearDown() {
        // remove all created test product accountings
        try {
            productAccountingsToDelete.forEach(s -> removeProductAccounting(s));
            // Clear list
            productAccountingsToDelete = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testCreateProductAccounting() {
        ProductAccountingDto productAccountingDtoMock = createMockProductAccountingDto();
        String productId = productAccountingDtoMock.getProductId();

        productAccountingController.createProductAccounting(
                productId,
                productAccountingDtoMock);
        ProductAccounting productAccounting = readProductAccounting(productId);

        assertNotNull(productAccounting);
        assertEquals(productAccountingDtoMock.getBalanceProfitCenter(), productAccounting.getBalanceProfitCenter());
        assertEquals(productAccountingDtoMock.getMainLedgerAccount(), productAccounting.getMainLedgerAccount());
        assertEquals(productAccountingDtoMock.getProject(), productAccounting.getProject());
        assertEquals(productAccountingDtoMock.getCompanyCode(), productAccounting.getCompanyCode());
        assertEquals(productAccountingDtoMock.getInternalOrder(), productAccounting.getInternalOrder());
        assertEquals(productAccountingDtoMock.getVatCode(), productAccounting.getVatCode());
        assertEquals(productAccountingDtoMock.getOperationArea(), productAccounting.getOperationArea());
        assertEquals(productAccountingDtoMock.getProfitCenter(), productAccounting.getProfitCenter());

    }

    private ProductAccountingDto createMockProductAccountingDto() {
        ProductAccountingDto productAccountingMock = new ProductAccountingDto();

        productAccountingMock.setProductId(createProductId());

        productAccountingMock.setMainLedgerAccount("1234");
        productAccountingMock.setProject("project");
        productAccountingMock.setCompanyCode("12345");
        productAccountingMock.setOperationArea("opArea");
        productAccountingMock.setVatCode("VAT10");
        productAccountingMock.setInternalOrder("order1");

        productAccountingMock.setProfitCenter("profitCenter");
        productAccountingMock.setBalanceProfitCenter("balanceProfitCenter");

        return productAccountingMock;
    }

    private void removeProductAccounting(String productId) {
        productAccountingRepository.deleteById(productId);
    }

    // read productAccounting from db
    private ProductAccounting readProductAccounting(String productId){
        Optional<ProductAccounting> productAccounting =  productAccountingRepository.findById(productId);

        return productAccounting.get();
    }

    // generate new product ID and add it to the list for tearDown
    private String createProductId(){
        String productId = UUID.randomUUID().toString();
        productAccountingsToDelete.add(productId);
        return productId;
    }

}
