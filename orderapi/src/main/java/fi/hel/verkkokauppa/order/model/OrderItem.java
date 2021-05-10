package fi.hel.verkkokauppa.order.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "orderitems")
public class OrderItem {
    @Id
    String orderItemId;
    @Field(type = FieldType.Keyword)
    String orderId;

    @Field(type = FieldType.Keyword)
    String productId;
    @Field(type = FieldType.Text)
    String productName;
    @Field(type = FieldType.Text)
    Integer quantity;
    @Field(type = FieldType.Text)
    String unit;

    @Field(type = FieldType.Text)
    String rowPriceNet;
    @Field(type = FieldType.Text)
    String rowPriceVat;
    @Field(type = FieldType.Text)
    String rowPriceTotal;

    public OrderItem() {}

    public OrderItem(String orderItemId, String orderId, String productId, String productName, Integer quantity,
            String unit, String rowPriceNet, String rowPriceVat, String rowPriceTotal) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unit = unit;
        this.rowPriceNet = rowPriceNet;
        this.rowPriceVat = rowPriceVat;
        this.rowPriceTotal = rowPriceTotal;
    }

    public String getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(String orderItemId) {
        this.orderItemId = orderItemId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getRowPriceNet() {
        return rowPriceNet;
    }

    public void setRowPriceNet(String rowPriceNet) {
        this.rowPriceNet = rowPriceNet;
    }

    public String getRowPriceVat() {
        return rowPriceVat;
    }

    public void setRowPriceVat(String rowPriceVat) {
        this.rowPriceVat = rowPriceVat;
    }

    public String getRowPriceTotal() {
        return rowPriceTotal;
    }

    public void setRowPriceTotal(String rowPriceTotal) {
        this.rowPriceTotal = rowPriceTotal;
    }

}
