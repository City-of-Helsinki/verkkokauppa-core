package fi.hel.verkkokauppa.message.enums;

public enum MessageTypes {
    EMAIL("email");
    public final String type;

    MessageTypes(String type) {
        this.type = type;
    }
}
