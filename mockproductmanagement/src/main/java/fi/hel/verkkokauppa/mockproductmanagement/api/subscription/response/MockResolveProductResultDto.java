package fi.hel.verkkokauppa.mockproductmanagement.api.subscription.response;

import lombok.Data;

@Data
public class MockResolveProductResultDto {
    private String subscriptionId;
    private String userId;
    private String productId;
    private String productName;
    private String productLabel;
    private String productDescription;
    private MockOrderItemMetaDto[] orderItemMetas;

}
