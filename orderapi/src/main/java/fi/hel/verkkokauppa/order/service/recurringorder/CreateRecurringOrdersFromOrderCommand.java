package fi.hel.verkkokauppa.order.service.recurringorder;

import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.api.data.recurringorder.MerchantDto;
import fi.hel.verkkokauppa.order.api.data.recurringorder.ProductDto;
import fi.hel.verkkokauppa.common.constants.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CreateRecurringOrdersFromOrderCommand {

	private final CreateRecurringOrderCommand createRecurringOrderCommand;

	@Autowired
	public CreateRecurringOrdersFromOrderCommand(
			CreateRecurringOrderCommand createRecurringOrderCommand
	) {
		this.createRecurringOrderCommand = createRecurringOrderCommand;
	}

	public Set<String> createFromOrder(OrderAggregateDto orderAggregateDto) {
		if (!canCreateFromOrder(orderAggregateDto)) {
			throw new IllegalArgumentException("Can't create recurring order from this order.");
		}

		Set<String> idList = new HashSet<>();

		for (OrderItemDto orderItemDto : orderAggregateDto.getItems()) {
			if (!canCreateFromOrderItem(orderItemDto)) {
				continue;
			}

			RecurringOrderDto recurringOrderDto = new RecurringOrderDto();

			copyOrderFieldsToRecurringOrder(orderAggregateDto.getOrder(), recurringOrderDto);
			copyOrderItemFieldsToRecurringOrder(orderItemDto, recurringOrderDto);

			String id = createRecurringOrderCommand.create(recurringOrderDto);
			idList.add(id);
		}

		return idList;
	}

	private void copyOrderFieldsToRecurringOrder(OrderDto order, RecurringOrderDto recurringOrderDto) {
		MerchantDto merchant = new MerchantDto();
		merchant.setName(order.getUser());
		merchant.setNamespace(order.getNamespace());

		recurringOrderDto.setRelatedOrderIds(new HashSet<>() {{ add(order.getOrderId()); }});
		recurringOrderDto.setCustomerId(order.getCustomerEmail());
		recurringOrderDto.setMerchant(merchant);
	}

	private void copyOrderItemFieldsToRecurringOrder(OrderItemDto orderItem, RecurringOrderDto recurringOrderDto) {
		ProductDto product = new ProductDto();
		product.setId(orderItem.getProductId());
		product.setName(orderItem.getProductName());

		recurringOrderDto.setProduct(product);
		recurringOrderDto.setQuantity(orderItem.getQuantity());
		recurringOrderDto.setStartDate(orderItem.getStartDate());
		recurringOrderDto.setPeriodFrequency(orderItem.getPeriodFrequency());
		recurringOrderDto.setPeriodUnit(orderItem.getPeriodUnit());
		//TODO Poista
//		recurringOrderDto.setPriceTotal(orderItem.getRowPriceTotal());
//		recurringOrderDto.setPriceVat(orderItem.getRowPriceVat());
//		recurringOrderDto.setPriceNet(orderItem.getRowPriceNet());
	}

	private boolean canCreateFromOrder(OrderAggregateDto orderAggregateDto) {
		return orderAggregateDto.getOrder().getType().equals(OrderType.SUBSCRIPTION);
	}

	private boolean canCreateFromOrderItem(OrderItemDto item) {
		return item.getPeriodUnit() != null && item.getPeriodFrequency() != null;
	}
}
