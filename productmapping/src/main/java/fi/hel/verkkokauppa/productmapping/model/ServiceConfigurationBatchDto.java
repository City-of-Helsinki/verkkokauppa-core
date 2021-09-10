package fi.hel.verkkokauppa.productmapping.model;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceConfigurationBatchDto {

    private String namespace;

    private Map<String, String> configurations;

    public ServiceConfigurationBatchDto() {
        namespace = null;
        configurations = new HashMap<>();
    }

}
