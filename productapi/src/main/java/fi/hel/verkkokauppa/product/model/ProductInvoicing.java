package fi.hel.verkkokauppa.product.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "invoicing")
@Data
public class ProductInvoicing {
    @Id
    private String productId;

    @Field(type = FieldType.Text)
    private String salesOrg;

    @Field(type = FieldType.Text)
    private String salesOffice;

    @Field(type = FieldType.Text)
    private String material;

    @Field(type = FieldType.Text)
    private String orderType;
}
