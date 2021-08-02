package fi.hel.verkkokauppa.product;

import fi.hel.verkkokauppa.product.dto.ProductAccountingDto;
import fi.hel.verkkokauppa.product.model.ProductAccounting;
import fi.hel.verkkokauppa.product.service.Transformer;

public class ProductAccountingTransformer implements Transformer<ProductAccountingDto, ProductAccounting> {

    @Override
    public ProductAccounting transform(ProductAccountingDto dto) {
        ProductAccounting productAccounting = new ProductAccounting();
        productAccounting.setProductId(dto.getProductId());
        productAccounting.setMainLedgerAccount(dto.getMainLedgerAccount());
        productAccounting.setProject(dto.getProject());
        productAccounting.setInternalOrder(dto.getInternalOrder());
        productAccounting.setProfitCenter(dto.getProfitCenter());
        productAccounting.setOperationArea(dto.getOperationArea());
        return productAccounting;
    }

    @Override
    public ProductAccountingDto transform(ProductAccounting productAccounting) {
        ProductAccountingDto dto = new ProductAccountingDto();
        dto.setProductId(productAccounting.getProductId());
        dto.setMainLedgerAccount(productAccounting.getMainLedgerAccount());
        dto.setProject(productAccounting.getProject());
        dto.setInternalOrder(productAccounting.getInternalOrder());
        dto.setProfitCenter(productAccounting.getProfitCenter());
        dto.setOperationArea(productAccounting.getOperationArea());
        return dto;
    }
}
