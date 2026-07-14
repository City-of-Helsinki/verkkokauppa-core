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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    Instant updatedAt;

    @Field(type = FieldType.Auto)
    ArrayList<ConfigurationModel> configurations;

    public LocalDateTime getCreatedAt(){
        return LocalDateTime.ofInstant(this.createdAt, ZoneOffset.UTC);
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt.atZone(ZoneOffset.UTC).toInstant();
    }

    public LocalDateTime getUpdatedAt() {
        return LocalDateTime.ofInstant(this.updatedAt, ZoneOffset.UTC);
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt.atZone(ZoneOffset.UTC).toInstant();
    }
}

