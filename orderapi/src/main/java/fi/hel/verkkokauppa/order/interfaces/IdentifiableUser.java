package fi.hel.verkkokauppa.order.interfaces;

public interface IdentifiableUser {
    String getNamespace();

    void setNamespace(String namespace);

    String getUser();

    void setUser(String user);
}
