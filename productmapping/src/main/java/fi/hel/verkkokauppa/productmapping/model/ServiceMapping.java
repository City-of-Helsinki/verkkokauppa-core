package fi.hel.verkkokauppa.productmapping.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

@KeySpace("services")
public class ServiceMapping {
    @Id
    String serviceId;
    String namespace;
    String type;
    String serviceUrl;

    public ServiceMapping() {
    }

    public ServiceMapping(String serviceId, String namespace, String type, String serviceUrl) {
        this.serviceId = serviceId;
        this.namespace = namespace;
        this.type = type;
        this.serviceUrl = serviceUrl;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }
    
}
