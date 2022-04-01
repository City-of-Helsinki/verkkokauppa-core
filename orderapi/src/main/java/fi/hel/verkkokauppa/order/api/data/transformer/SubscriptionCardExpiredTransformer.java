package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCardExpiredDto;
import fi.hel.verkkokauppa.order.model.subscription.email.SubscriptionCardExpired;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionCardExpiredTransformer implements ITransformer<SubscriptionCardExpired, SubscriptionCardExpiredDto> {

    private final ModelMapper modelMapper = new ModelMapper();

    @Override
    public SubscriptionCardExpired transformToEntity(SubscriptionCardExpiredDto cardExpiredDto) {
        return modelMapper.map(cardExpiredDto, SubscriptionCardExpired.class);
    }

    @Override
    public SubscriptionCardExpiredDto transformToDto(SubscriptionCardExpired entity) {
        return modelMapper.map(entity, SubscriptionCardExpiredDto.class);
    }

}