package fi.hel.verkkokauppa.history.model;

import fi.hel.verkkokauppa.common.contracts.history.History;
import lombok.Builder;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "history")
@Data
@Builder
public class HistoryModel implements History {
    @Id
    String historyId;
    // Identifier for entity example, orderId
    @Field(type = FieldType.Text)
    String entityId;

    @Field(type = FieldType.Text)
    String user;

    @Field(type = FieldType.Boolean)
    @Builder.Default
    Boolean isVisible = false;

    @Field(type = FieldType.Keyword)
    String entityType;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    LocalDateTime createdAt;

    @Field(type = FieldType.Keyword)
    String namespace;

    @Field(type = FieldType.Keyword)
    String eventType;

    @Field(type = FieldType.Keyword)
    String payload;

    @Field(type = FieldType.Keyword)
    String description;
}
