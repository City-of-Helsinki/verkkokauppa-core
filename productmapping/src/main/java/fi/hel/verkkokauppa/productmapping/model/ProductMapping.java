package fi.hel.verkkokauppa.productmapping.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "productmappings")
public class ProductMapping {
    @Id
    String productId;
    @Field(type = FieldType.Keyword)
    String namespace;
    @Field(type = FieldType.Text)
    String namespaceEntityId;

    public ProductMapping() {
    }
    
    public ProductMapping(String productId, String namespace, String namespaceEntityId) {
        this.productId = productId;
        this.namespace = namespace;
        this.namespaceEntityId = namespaceEntityId;
    }
    
    public String getProductId() {
        return productId;
    }
    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespaceEntityId() {
        return namespaceEntityId;
    }

    public void setNamespaceEntityId(String namespaceEntityId) {
        this.namespaceEntityId = namespaceEntityId;
    }

}
