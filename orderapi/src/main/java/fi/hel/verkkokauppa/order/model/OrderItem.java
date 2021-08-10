package fi.hel.verkkokauppa.order.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

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

    @Field(type = FieldType.Text)
    String vatPercentage;
    @Field(type = FieldType.Text)
    String priceNet;
    @Field(type = FieldType.Text)
    String priceVat;
    @Field(type = FieldType.Text)
    String priceGross;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate; // TODO: aika myös?

    @Field(type = FieldType.Text)
    private String periodUnit;

    @Field(type = FieldType.Long)
    private Long periodFrequency;

    public OrderItem() {}

    public OrderItem(String orderItemId, String orderId, String productId, String productName, Integer quantity,
            String unit, String rowPriceNet, String rowPriceVat, String rowPriceTotal, String vatPercentage, String priceNet, String priceVat, String priceGross) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unit = unit;
        this.rowPriceNet = rowPriceNet;
        this.rowPriceVat = rowPriceVat;
        this.rowPriceTotal = rowPriceTotal;
        this.vatPercentage = vatPercentage;
        this.priceNet = priceNet;
        this.priceVat = priceVat;
        this.priceGross = priceGross;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getPeriodUnit() {
        return periodUnit;
    }

    public void setPeriodUnit(String periodUnit) {
        this.periodUnit = periodUnit;
    }

    public Long getPeriodFrequency() {
        return periodFrequency;
    }

    public void setPeriodFrequency(Long periodFrequency) {
        this.periodFrequency = periodFrequency;
    }

    public String getVatPercentage() { return vatPercentage; }

    public void setVatPercentage(String vatPercentage) { this.vatPercentage = vatPercentage; }

    public String getPriceNet() { return priceNet; }

    public void setPriceNet(String priceNet) { this.priceNet = priceNet; }

    public String getPriceVat() { return priceVat; }

    public void setPriceVat(String priceVat) { this.priceVat = priceVat; }

    public String getPriceGross() { return priceGross; }

    public void setPriceGross(String priceGross) { this.priceGross = priceGross; }
}
