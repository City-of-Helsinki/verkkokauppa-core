package fi.hel.verkkokauppa.common.history.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
public class HistoryModel {
    @Id
    String entityId;

    @Field(type = FieldType.Boolean)
    @Builder.Default
    Boolean isVisible = false;

    @Field(type = FieldType.Keyword)
    String entityType;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    DateTime createdAt;

    @Field(type = FieldType.Keyword)
    String namespace;

    @Field(type = FieldType.Text)
    String eventType;

    @Field(type = FieldType.Keyword)
    String payload;

    @Field(type = FieldType.Keyword)
    String description;
}
