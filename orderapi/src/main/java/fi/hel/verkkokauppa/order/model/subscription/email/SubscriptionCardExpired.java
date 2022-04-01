package fi.hel.verkkokauppa.order.model.subscription.email;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "subscription_card_expired")
@Data
@Builder
public class SubscriptionCardExpired {
    @Id
    String subscriptionCardExpiredId;

    @Field(type = FieldType.Text)
    private String subscriptionId;

    @CreatedDate
    private LocalDateTime createdAt;
}
