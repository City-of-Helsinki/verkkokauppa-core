package fi.hel.verkkokauppa.product;

import fi.hel.verkkokauppa.product.dto.ProductAccountingDto;
import fi.hel.verkkokauppa.product.model.ProductAccounting;
import fi.hel.verkkokauppa.product.service.Transformer;

public class ProductAccountingTransformer implements Transformer<ProductAccountingDto, ProductAccounting> {

    @Override
    public ProductAccounting transform(ProductAccountingDto dto) {
        ProductAccounting productAccounting = new ProductAccounting();
        productAccounting.setProductId(dto.getProductId());
        productAccounting.setCompanyCode(dto.getCompanyCode());
        productAccounting.setMainLedgerAccount(dto.getMainLedgerAccount());
        productAccounting.setVatCode(dto.getVatCode());
        productAccounting.setProject(dto.getProject());
        productAccounting.setInternalOrder(dto.getInternalOrder());
        productAccounting.setProfitCenter(dto.getProfitCenter());
        productAccounting.setBalanceProfitCenter(dto.getBalanceProfitCenter());
        productAccounting.setOperationArea(dto.getOperationArea());
        productAccounting.setActiveFrom(dto.getActiveFrom());
        productAccounting.setNextEntity(dto.getNextEntity());
        productAccounting.setNextEntity(dto.getNextEntity());
        productAccounting.setNamespace(dto.getNamespace());
        return productAccounting;
    }

    @Override
    public ProductAccountingDto transform(ProductAccounting productAccounting) {
        ProductAccountingDto dto = new ProductAccountingDto();
        dto.setProductId(productAccounting.getProductId());
        dto.setCompanyCode(productAccounting.getCompanyCode());
        dto.setMainLedgerAccount(productAccounting.getMainLedgerAccount());
        dto.setVatCode(productAccounting.getVatCode());
        dto.setProject(productAccounting.getProject());
        dto.setInternalOrder(productAccounting.getInternalOrder());
        dto.setProfitCenter(productAccounting.getProfitCenter());
        dto.setBalanceProfitCenter(productAccounting.getBalanceProfitCenter());
        dto.setOperationArea(productAccounting.getOperationArea());
        dto.setNextEntity(productAccounting.getNextEntity());
        dto.setActiveFrom(productAccounting.getActiveFrom());
        dto.setNamespace(productAccounting.getNamespace());
        return dto;
    }
}
