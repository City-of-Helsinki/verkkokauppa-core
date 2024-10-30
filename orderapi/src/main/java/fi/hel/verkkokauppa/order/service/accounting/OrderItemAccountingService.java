package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.util.IterableUtils;
import fi.hel.verkkokauppa.order.api.data.accounting.CreateOrderAccountingRequestDto;
import fi.hel.verkkokauppa.order.api.data.accounting.OrderItemAccountingDto;
import fi.hel.verkkokauppa.order.api.data.accounting.ProductAccountingDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemAccountingTransformer;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.accounting.OrderItemAccounting;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemAccountingRepository;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderItemAccountingService {

    private Logger log = LoggerFactory.getLogger(OrderItemAccountingService.class);

    @Autowired
    private OrderItemAccountingRepository orderItemAccountingRepository;

    @Autowired
    private OrderItemService orderItemService;

    public OrderItemAccounting createOrderItemAccounting(OrderItemAccountingDto orderItemAccountingDto) {
        OrderItemAccounting orderItemAccountingEntity = new OrderItemAccountingTransformer().transformToEntity(orderItemAccountingDto);
        this.orderItemAccountingRepository.save(orderItemAccountingEntity);
        return orderItemAccountingEntity;
    }

    public List<OrderItemAccountingDto> createOrderItemAccountings(CreateOrderAccountingRequestDto request) {
        List<OrderItemAccountingDto> orderItemAccountings = new ArrayList<>();

        String orderId = request.getOrderId();
        List<ProductAccountingDto> productAccountingDtos = request.getDtos();
        List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
        for (OrderItem orderItem : orderItems) {
            String orderItemProductId = orderItem.getProductId();

            for (ProductAccountingDto productAccountingDto : productAccountingDtos) {
                String productId = productAccountingDto.getProductId();

                if (productId.equalsIgnoreCase(orderItemProductId)) {
                    String orderItemId = orderItem.getOrderItemId();
                    String priceGross = orderItem.getRowPriceTotal();
                    String priceNet = orderItem.getRowPriceNet();
                    String priceVat = orderItem.getRowPriceVat();
                    OrderItemAccountingDto orderItemAccountingDto = new OrderItemAccountingDto(orderItemId, orderId, priceGross,
                            priceNet, priceVat, productAccountingDto);
                    // TODO add data using setters
                    orderItemAccountingDto.setPaidAt(productAccountingDto.getPaidAt());
                    orderItemAccountingDto.setNamespace(productAccountingDto.getNamespace());
                    orderItemAccountingDto.setMerchantId(productAccountingDto.getMerchantId());
                    orderItemAccountingDto.setPaytrailTransactionId(productAccountingDto.getPaytrailTransactionId());


                    createOrderItemAccounting(orderItemAccountingDto);
                    orderItemAccountings.add(orderItemAccountingDto);
                }
            }
        }
        return orderItemAccountings;
    }

    public OrderItemAccounting getOrderItemAccounting(String orderItemId) {
        Optional<OrderItemAccounting> mapping = orderItemAccountingRepository.findById(orderItemId);

        if (mapping.isPresent())
            return mapping.get();

        log.warn("product accounting not found, orderItemId: " + orderItemId);
        return null;
    }

    public List<OrderItemAccounting> getOrderItemAccountings(String orderId) {
        List<OrderItemAccounting> accountings = orderItemAccountingRepository.findByOrderId(orderId);

        if (accountings.size() > 0)
            return accountings;

        log.debug("orderItems not found, orderId: " + orderId);
        return new ArrayList<OrderItemAccounting>();
    }

    public List<OrderItemAccounting> getOrderItemAccountings() {
        List<OrderItemAccounting> accountings = IterableUtils.iterableToList(orderItemAccountingRepository.findAll());

        if (accountings.size() > 0)
            return accountings;

        log.debug("orderItems not found");
        return new ArrayList<OrderItemAccounting>();
    }
}
