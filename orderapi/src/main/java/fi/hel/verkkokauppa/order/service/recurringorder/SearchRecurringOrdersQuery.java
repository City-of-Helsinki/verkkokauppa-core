package fi.hel.verkkokauppa.order.service.recurringorder;

import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderCriteria;
import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.logic.RecurringOrderMappingLogic;
import fi.hel.verkkokauppa.order.model.recurringorder.RecurringOrder;
import fi.hel.verkkokauppa.order.model.recurringorder.RecurringOrderQueryBuilderBuilder;
import fi.hel.verkkokauppa.order.repository.jpa.RecurringOrderRepository;
import fi.hel.verkkokauppa.shared.mapper.ListMapper;
import fi.hel.verkkokauppa.shared.service.DefaultSearchEntitiesQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchRecurringOrdersQuery extends
		DefaultSearchEntitiesQuery<RecurringOrder, RecurringOrderDto, RecurringOrderCriteria, String> {

	private final RecurringOrderMappingLogic recurringOrderMappingLogic;

	@Autowired
	public SearchRecurringOrdersQuery(
			RecurringOrderRepository repository,
			RecurringOrderQueryBuilderBuilder recurringOrderQueryBuilder,
			ListMapper listMapper,
			RecurringOrderMappingLogic recurringOrderMappingLogic
	) {
		super(repository, recurringOrderQueryBuilder, listMapper, RecurringOrderDto.class);
		this.recurringOrderMappingLogic = recurringOrderMappingLogic;
	}

	public List<RecurringOrderDto> searchActive(RecurringOrderCriteria criteria) {
		return super.search(criteria);
	}

	protected void mapItemToDto(RecurringOrder entity, RecurringOrderDto dto) {
		recurringOrderMappingLogic.mapMerchantDataToDto(entity, dto);
		recurringOrderMappingLogic.mapProductDataToDto(entity, dto);
		//recurringOrderMappingLogic.mapBillingAddressDataToDto(entity, dto); // TODO?
		//recurringOrderMappingLogic.mapShippingAddressDataToDto(entity, dto); // TODO?
	}
}
