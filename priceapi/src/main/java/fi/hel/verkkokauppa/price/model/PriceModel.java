package fi.hel.verkkokauppa.price.model;

import com.fasterxml.jackson.annotation.JsonRawValue;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.price.service.PriceService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "price")
@Data
@AllArgsConstructor
public class PriceModel {
    @Id
    String id;

    @Field(type = FieldType.Text)
    String productId;

    @Field(type = FieldType.Text)
    String price;

    @Field(type = FieldType.Text)
    String grossValue;

    @Field(type = FieldType.Text)
    String netValue;

    @Field(type = FieldType.Text)
    String vatValue;

    @Transient
    @JsonRawValue
    JSONObject original;

    @Field(type = FieldType.Text)
    String vatPercentage = "24.0";

    public PriceModel() {}

    public PriceModel(String productId, String price, JSONObject original) {
        this.id = UUIDGenerator.generateType4UUID().toString();
        this.productId = productId;
        this.price = price.replace(",", ".");
        this.grossValue = price.replace(",", ".");
        this.original = original;
        double grossValue = Double.parseDouble(this.grossValue);
        double[] result = PriceService.calculateNetAndVat(grossValue, Double.parseDouble(vatPercentage));

        double netValue = result[0];
        double vatValue = result[1];
        this.netValue = Double.toString(netValue);
        this.vatValue = Double.toString(vatValue);
    }
    
}
