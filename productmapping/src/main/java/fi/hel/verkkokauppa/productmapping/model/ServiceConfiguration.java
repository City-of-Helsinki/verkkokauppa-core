package fi.hel.verkkokauppa.productmapping.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;


@Document(indexName = "serviceconfigurations")
public class ServiceConfiguration {
        @Id
        String configurationId;
        @Field(type = FieldType.Keyword)
        String namespace;
        @Field(type = FieldType.Keyword)
        String configurationKey;
        @Field(type = FieldType.Text)
        String configurationValue;
    
        public ServiceConfiguration() {
        }

        public ServiceConfiguration(String configurationId, String namespace, String configurationKey,
                String configurationValue) {
            this.configurationId = configurationId;
            this.namespace = namespace;
            this.configurationKey = configurationKey;
            this.configurationValue = configurationValue;
        }

        public String getConfigurationId() {
            return configurationId;
        }

        public void setConfigurationId(String configurationId) {
            this.configurationId = configurationId;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }

        public void setConfigurationKey(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationValue() {
            return configurationValue;
        }

        public void setConfigurationValue(String configurationValue) {
            this.configurationValue = configurationValue;
        }

}
