package fi.hel.verkkokauppa.order.api.data.subscription;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionIdsDto {
    private Set<String> subscriptionIds;

}
