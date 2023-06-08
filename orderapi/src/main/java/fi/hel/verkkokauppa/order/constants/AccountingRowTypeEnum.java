package fi.hel.verkkokauppa.order.constants;

public enum AccountingRowTypeEnum {
    ORDER("order"),
    REFUND("refund");

    private final String rowType;

    private AccountingRowTypeEnum(String rowType) {
        this.rowType = rowType;
    }

    public String getRowType() {
        return rowType;
    }

    @Override
    public String toString() {
        return rowType;
    }
}
