package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.order.api.data.accounting.NextAccountingEntityDto;
import fi.hel.verkkokauppa.order.api.data.accounting.ProductAccountingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProductAccountingDtoTest {

    private ProductAccountingDto dto;
    private NextAccountingEntityDto nextEntity;

    @BeforeEach
    public void setUp() {
        // Initialize the ProductAccountingDto and NextAccountingEntityDto objects
        dto = new ProductAccountingDto();
        nextEntity = new NextAccountingEntityDto();

        // Set values in dto
        dto.setProductId("P123");
        dto.setCompanyCode("C001");
        dto.setMainLedgerAccount("MLA123");
        dto.setVatCode("VAT001");
        dto.setInternalOrder("IO123");
        dto.setProfitCenter("PC123");
        dto.setBalanceProfitCenter("BPC123");
        dto.setProject("Proj123");
        dto.setOperationArea("OA123");
        dto.setNamespace("Namespace123");

        // Set values in nextEntity
//        nextEntity.setProductId("P456");
        nextEntity.setCompanyCode("C002");
        nextEntity.setMainLedgerAccount("MLA456");
        nextEntity.setVatCode("VAT002");
        nextEntity.setInternalOrder("IO456");
        nextEntity.setProfitCenter("PC456");
        nextEntity.setBalanceProfitCenter("BPC456");
        nextEntity.setProject("Proj456");
        nextEntity.setOperationArea("OA456");

        // Assign nextEntity to dto
        dto.setNextEntity(nextEntity);
    }

    @Test
    public void testGettersBeforeActiveFromExceeded() {
        // Set activeFrom in the future
        dto.setActiveFrom(LocalDateTime.now().plusDays(1));

        // Assert values should come from dto
        assertEquals("P123", dto.getProductId());
        assertEquals("C001", dto.getCompanyCode());
        assertEquals("MLA123", dto.getMainLedgerAccount());
        assertEquals("VAT001", dto.getVatCode());
        assertEquals("IO123", dto.getInternalOrder());
        assertEquals("PC123", dto.getProfitCenter());
        assertEquals("BPC123", dto.getBalanceProfitCenter());
        assertEquals("Proj123", dto.getProject());
        assertEquals("OA123", dto.getOperationArea());
        assertEquals("Namespace123", dto.getNamespace());
    }

    @Test
    public void testGettersAfterActiveFromExceeded() {
        // Set activeFrom in the past
        dto.setActiveFrom(LocalDateTime.now().minusDays(1));

        // Assert values should come from nextEntity
//        assertEquals("P456", dto.getProductId());
        assertEquals("C002", dto.getCompanyCode());
        assertEquals("MLA456", dto.getMainLedgerAccount());
        assertEquals("VAT002", dto.getVatCode());
        assertEquals("IO456", dto.getInternalOrder());
        assertEquals("PC456", dto.getProfitCenter());
        assertEquals("BPC456", dto.getBalanceProfitCenter());
        assertEquals("Proj456", dto.getProject());
        assertEquals("OA456", dto.getOperationArea());
        assertEquals("Namespace123", dto.getNamespace());
    }

    @Test
    public void testGettersNoNextEntity() {
        // Set activeFrom in the past but no nextEntity
        dto.setActiveFrom(LocalDateTime.now().minusDays(1));
        dto.setNextEntity(null);

        // Assert values should still come from dto
        assertEquals("P123", dto.getProductId());
        assertEquals("C001", dto.getCompanyCode());
        assertEquals("MLA123", dto.getMainLedgerAccount());
        assertEquals("VAT001", dto.getVatCode());
        assertEquals("IO123", dto.getInternalOrder());
        assertEquals("PC123", dto.getProfitCenter());
        assertEquals("BPC123", dto.getBalanceProfitCenter());
        assertEquals("Proj123", dto.getProject());
        assertEquals("OA123", dto.getOperationArea());
        assertEquals("Namespace123", dto.getNamespace());
    }

    @Test
    public void testGettersFallbackToDtoValueWhenNextEntityValueIsNull() {
        // Set activeFrom in the past
        dto.setActiveFrom(LocalDateTime.now().minusDays(1));

        // Explicitly set nextEntity values to null where fallback is expected
        nextEntity.setCompanyCode(null);
        nextEntity.setMainLedgerAccount(null);
        nextEntity.setVatCode(null);
        nextEntity.setInternalOrder(null);
        nextEntity.setProfitCenter(null);
        nextEntity.setBalanceProfitCenter(null);
        nextEntity.setProject(null);
        nextEntity.setOperationArea(null);

        // Assert that the values fall back to the original dto values
        assertEquals("P123", dto.getProductId());
        assertEquals("C001", dto.getCompanyCode());
        assertEquals("MLA123", dto.getMainLedgerAccount());
        assertEquals("VAT001", dto.getVatCode());
        assertEquals("IO123", dto.getInternalOrder());
        assertEquals("PC123", dto.getProfitCenter());
        assertEquals("BPC123", dto.getBalanceProfitCenter());
        assertEquals("Proj123", dto.getProject());
        assertEquals("OA123", dto.getOperationArea());
    }
}
