package fi.hel.verkkokauppa.configuration.model.namespace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonMerge;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
@Document(indexName = "namespace")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamespaceModel {
    @Id
    String namespaceId;

    @Field(type = FieldType.Text)
    String namespace;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    LocalDateTime updatedAt;

    @Field(type = FieldType.Auto)
    ArrayList<ConfigurationModel> configurations;
}

