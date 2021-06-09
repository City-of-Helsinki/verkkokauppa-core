package fi.hel.verkkokauppa.order.service.recurringorder;

import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.api.data.recurringorder.MerchantDto;
import fi.hel.verkkokauppa.order.api.data.recurringorder.ProductDto;
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

	public Set<String> createFromOrder(OrderDto dto) {
		if (!canCreateFromOrder(dto)) {
			throw new IllegalArgumentException("Can't create recurring order from this order.");
		}

		Set<String> idList = new HashSet<>();

		for (OrderItem item : dto.getItems()) {
			RecurringOrderDto recurringOrderDto = new RecurringOrderDto();

			copyOrderFieldsToRecurringOrder(dto.getOrder(), recurringOrderDto);
			copyOrderItemFieldsToRecurringOrder(item, recurringOrderDto);

			String id = createRecurringOrderCommand.create(recurringOrderDto); // TODO: what if fails? catch exception?
			idList.add(id);
		}

		return idList;
	}

	private void copyOrderFieldsToRecurringOrder(Order order, RecurringOrderDto recurringOrderDto) {
		MerchantDto merchant = new MerchantDto();
		merchant.setName(order.getUser());
		merchant.setNamespace(order.getNamespace());

		recurringOrderDto.setRelatedOrderIds(new HashSet<>() {{ add(order.getOrderId()); }});
		recurringOrderDto.setCustomerId(order.getCustomerEmail()); // TODO: ok?
		recurringOrderDto.setMerchant(merchant);
	}

	private void copyOrderItemFieldsToRecurringOrder(OrderItem orderItem, RecurringOrderDto recurringOrderDto) {
		ProductDto product = new ProductDto();
		product.setId(orderItem.getProductId());
		product.setName(orderItem.getProductName());

		recurringOrderDto.setProduct(product);
		recurringOrderDto.setQuantity(orderItem.getQuantity());
		recurringOrderDto.setStartDate(orderItem.getStartDate());
		recurringOrderDto.setPeriodFrequency(orderItem.getPeriodFrequency());
		recurringOrderDto.setPeriodUnit(orderItem.getPeriodUnit());
		recurringOrderDto.setPriceTotal(orderItem.getRowPriceTotal());
		recurringOrderDto.setPriceVat(orderItem.getRowPriceVat());
		recurringOrderDto.setPriceNet(orderItem.getRowPriceNet());
	}

	private boolean canCreateFromOrder(OrderDto orderDto) {
		return orderDto.getOrder().getType().equals("subscription"); // TODO: ok? move to constant
	}
}
