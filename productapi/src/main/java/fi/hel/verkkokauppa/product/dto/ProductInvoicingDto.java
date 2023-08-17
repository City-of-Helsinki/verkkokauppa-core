package fi.hel.verkkokauppa.product.dto;

import lombok.Data;

@Data
public class ProductInvoicingDto {
    private String productId;
    private String salesOrg;
    private String salesOffice;
    private String material;
    private String orderType;
}
