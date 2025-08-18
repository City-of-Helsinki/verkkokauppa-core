package fi.hel.verkkokauppa.order.test.utils.payment;

import lombok.Data;
import java.util.List;


@Data
public class TestPaytrailPaymentResponse {
    private String transactionId;
    private String href;
    private String reference;
    private String terms;
    private List<Group> groups;
    private List<Provider> providers;

    @Data
    public static class Group {
        private String id;
        private String name;
        private String icon;
        private String svg;
    }

    @Data
    public static class Provider {
        private String name;
        private String url;
        private String icon;
        private String svg;
        private String id;
        private String group;
        private List<Parameter> parameters;
    }

    @Data
    public static class Parameter {
        private String name;
        private String value;
    }
}
