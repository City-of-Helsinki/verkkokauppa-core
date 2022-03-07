package fi.hel.verkkokauppa.order.api.data.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionDtoListWrapper {
    private List<SubscriptionDto> subscriptions;

}
