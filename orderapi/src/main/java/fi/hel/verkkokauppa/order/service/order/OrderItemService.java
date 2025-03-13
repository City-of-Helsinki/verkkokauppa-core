package fi.hel.verkkokauppa.order.service.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import org.modelmapper.internal.bytebuddy.implementation.bytecode.Throw;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;

@Component
public class OrderItemService {

    private Logger log = LoggerFactory.getLogger(OrderItemService.class);

    @Autowired
    private OrderItemRepository orderItemRepository;


    // updates existing order item from order or creates new one if one doe not already exist
    public String addOrUpdateItem(String orderId, String merchantId, String productId, String productName, String productLabel, String productDescription, Integer quantity, String unit,
                                  String rowPriceNet, String rowPriceVat, String rowPriceTotal, String vatPercentage, String priceNet, String priceVat, String priceGross,
                                  String originalPriceNet, String originalPriceVat, String originalPriceGross, String periodUnit, Long periodFrequency, Integer periodCount, LocalDateTime billingStartDate, LocalDateTime startDate, LocalDate invoicingDate) throws Exception {
        String orderItemId = null;
        List<OrderItem> orderItems = findByOrderId(orderId);
        log.debug("Updating or creating orderItem. " + orderItems.size() + " orderItems found for order : " + orderId);

        if( orderItems.size() == 1 )
        {
            // update the first one
            OrderItem orderItem = orderItems.get(0);
            orderItem.setMerchantId(merchantId);
            orderItem.setProductId(productId);
            orderItem.setProductName(productName);
            orderItem.setProductDescription(productDescription);
            orderItem.setProductLabel(productLabel);
            orderItem.setQuantity(quantity);
            orderItem.setUnit(unit);
            orderItem.setRowPriceNet(rowPriceNet);
            orderItem.setRowPriceVat(rowPriceVat);
            orderItem.setRowPriceTotal(rowPriceTotal);
            orderItem.setVatPercentage(vatPercentage);
            orderItem.setPriceNet(priceNet);
            orderItem.setPriceVat(priceVat);
            orderItem.setPriceGross(priceGross);
            orderItem.setOriginalPriceNet(originalPriceNet);
            orderItem.setOriginalPriceVat(originalPriceVat);
            orderItem.setOriginalPriceGross(originalPriceGross);
            orderItem.setPeriodUnit(periodUnit);
            orderItem.setPeriodFrequency(periodFrequency);
            orderItem.setPeriodCount(periodCount);
            orderItem.setBillingStartDate(billingStartDate);
            orderItem.setStartDate(startDate);
            orderItem.setInvoicingDate(invoicingDate);

            orderItemRepository.save(orderItem);
            orderItemId = orderItem.getOrderItemId();
            log.debug("orderItem updated, orderItemId: " + orderItemId);
        }
        else //if( orderItems.size() == 0 )
        {
            // orderitem did not exist so create one
            orderItemId = addItem(orderId, merchantId, productId, productName, productLabel, productDescription,
                    quantity, unit, rowPriceNet, rowPriceVat, rowPriceTotal, vatPercentage, priceNet, priceVat,
                    priceGross, originalPriceNet, originalPriceVat, originalPriceGross, periodUnit, periodFrequency,
                    periodCount, billingStartDate, startDate, invoicingDate);
        }
//        else
//        {
//         // TODO: enable later
//            log.error("Too many order items found with orderId: " + orderId);
//            throw new Exception("Too many order items found with orderId: "+ orderId);
//        }

        return orderItemId;
    }

    public String addItem(String orderId, String merchantId, String productId, String productName, String productLabel, String productDescription, Integer quantity, String unit,
                          String rowPriceNet, String rowPriceVat, String rowPriceTotal, String vatPercentage, String priceNet, String priceVat, String priceGross,
                          String originalPriceNet, String originalPriceVat, String originalPriceGross, String periodUnit, Long periodFrequency, Integer periodCount, LocalDateTime billingStartDate, LocalDateTime startDate, LocalDate invoicingDate) {
        String orderItemId = UUIDGenerator.generateType4UUID().toString();
        OrderItem orderItem = new OrderItem(
                orderItemId,
                orderId,
                merchantId,
                productId,
                productName,
                productLabel,
                productDescription,
                quantity,
                unit,
                rowPriceNet,
                rowPriceVat,
                rowPriceTotal,
                vatPercentage,
                priceNet,
                priceVat,
                priceGross,
                originalPriceNet,
                originalPriceVat,
                originalPriceGross,
                periodUnit,
                periodFrequency,
                periodCount,
                billingStartDate,
                startDate,
                invoicingDate
        );
        orderItemRepository.save(orderItem);

        log.debug("created new orderItem, orderItemId: " + orderItemId);
        return orderItemId;
    }

    public OrderItem findById(String orderItemId) {
        Optional<OrderItem> mapping = orderItemRepository.findById(orderItemId);

        if (mapping.isPresent())
            return mapping.get();

        log.debug("orderItem not found, orderItemId: " + orderItemId);
        return null;
    }

    public List<OrderItem> findByOrderId(String orderId) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        if (orderItems.size() > 0)
            return orderItems;

        log.debug("orderItems not found, orderId: " + orderId);
        return new ArrayList<OrderItem>();
    }

    public OrderItem setInvoicingStatus(String orderItemId, OrderItemInvoicingStatus status) {
        OrderItem item = orderItemRepository.findById(orderItemId).orElseThrow(() -> new CommonApiException(
                HttpStatus.NOT_FOUND,
                new Error("order-item-not-found", "order item with value: [" + orderItemId + "] not found")
        ));
        item.setInvoicingStatus(status);
        return orderItemRepository.save(item);
    }
}
