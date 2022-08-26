package fi.hel.verkkokauppa.common.constants;

public enum TypePrefix {
    ORDER("1"),
    INVOICE("2");

    public final String number;

    TypePrefix(String number) {
        this.number = number;
    }

}
