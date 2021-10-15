package fi.hel.verkkokauppa.order.interfaces;

public interface Customer {
    String getCustomerFirstName();

    String getCustomerLastName();

    String getCustomerEmail();

    String getCustomerPhone();

    void setCustomerFirstName(String customerFirstName);

    void setCustomerLastName(String customerLastName);

    void setCustomerEmail(String customerEmail);

    void setCustomerPhone(String customerPhone);
}
