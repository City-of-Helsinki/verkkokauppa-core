package fi.hel.verkkokauppa.order.model.subscription.email;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Document(indexName = "subscription_card_expired")
@Data
@Builder
public class SubscriptionCardExpired {
    @Id
    String subscriptionCardExpiredId;

    @Field(type = FieldType.Text)
    private String subscriptionId;

    @Field(type = FieldType.Text)
    private String namespace;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    public LocalDateTime getCreatedAt(){
        return LocalDateTime.ofInstant(this.createdAt, ZoneOffset.UTC);
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt.atZone(ZoneOffset.UTC).toInstant();
    }
}
