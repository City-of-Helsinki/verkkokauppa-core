package fi.hel.verkkokauppa.order.service.recurringorder;

import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.logic.RecurringOrderMappingLogic;
import fi.hel.verkkokauppa.order.model.recurringorder.RecurringOrder;
import fi.hel.verkkokauppa.order.repository.jpa.RecurringOrderRepository;
import fi.hel.verkkokauppa.shared.mapper.ObjectMapper;
import fi.hel.verkkokauppa.shared.service.DefaultGetEntityQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetRecurringOrderQuery extends DefaultGetEntityQuery<RecurringOrder, RecurringOrderDto, String> {

	private final RecurringOrderMappingLogic recurringOrderMappingLogic;

	@Autowired
	public GetRecurringOrderQuery(
			RecurringOrderRepository repository,
			ObjectMapper objectMapper,
			RecurringOrderMappingLogic recurringOrderMappingLogic
	) {
		super(repository, objectMapper, RecurringOrderDto.class);
		this.recurringOrderMappingLogic = recurringOrderMappingLogic;
	}

	@Override
	protected RecurringOrderDto mapToDto(RecurringOrder entity) {
		final RecurringOrderDto dto = super.mapToDto(entity);

		recurringOrderMappingLogic.mapMerchantDataToDto(entity, dto);
		recurringOrderMappingLogic.mapProductDataToDto(entity, dto);
		//recurringOrderMappingLogic.mapBillingAddressDataToDto(entity, dto); // TODO?
		//recurringOrderMappingLogic.mapShippingAddressDataToDto(entity, dto); // TODO?

		return dto;
	}
}
