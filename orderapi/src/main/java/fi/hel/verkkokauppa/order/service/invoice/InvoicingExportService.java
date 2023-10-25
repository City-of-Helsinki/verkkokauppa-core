package fi.hel.verkkokauppa.order.service.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.hel.verkkokauppa.common.id.IncrementId;
import fi.hel.verkkokauppa.order.api.data.invoice.xml.LineItem;
import fi.hel.verkkokauppa.order.api.data.invoice.xml.Party;
import fi.hel.verkkokauppa.order.api.data.invoice.xml.SalesOrder;
import fi.hel.verkkokauppa.order.api.data.invoice.xml.SalesOrderContainer;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicing;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Component
@Slf4j
public class InvoicingExportService {
    final IncrementId incrementId;

    final OrderItemRepository orderItemRepository;

    @Autowired
    public InvoicingExportService(IncrementId incrementId, OrderItemRepository orderItemRepository) {
        this.incrementId = incrementId;
        this.orderItemRepository = orderItemRepository;
    }

    private Party orderItemInvoicingToParty(OrderItemInvoicing item, boolean isOrderParty) {
        Party party = new Party();

        party.setCustomerYid(truncate(item.getCustomerYid(), 9));
        party.setCustomerOvt(truncate(item.getCustomerOvt(), 17));
        // OrderParty should only have ovt and yid
        if (!isOrderParty) {
            party.setPriorityName1(truncate(item.getCustomerName(), 35));
            party.setPriorityAddress1(truncate(item.getCustomerAddress(), 35));
            party.setPriorityCity(truncate(item.getCustomerCity(), 35));
            party.setPriorityPostalcode(truncate(item.getCustomerPostcode(), 9));
        }

        return party;
    }

    private boolean partyAreEqual(Party p1, Party p2) {
        return p1.getCustomerYid().equals(p2.getCustomerYid()) && p1.getCustomerOvt().equals(p2.getCustomerOvt()) &&
                p1.getPriorityName1().equals(p2.getPriorityName1()) && p1.getPriorityAddress1().equals(p2.getPriorityAddress1()) &&
                p1.getPriorityCity().equals(p2.getPriorityCity()) && p1.getPriorityPostalcode().equals(p2.getPriorityPostalcode());
    }

    private boolean productInvoicingAreEqual(SalesOrder salesOrder, OrderItemInvoicing item) {
        return salesOrder.getOrderType().equals(item.getOrderType()) && salesOrder.getSalesOrg().equals(item.getSalesOrg()) &&
                salesOrder.getSalesOffice().equals(item.getSalesOffice()) && salesOrder.getPoNumber().equals(item.getOrderIncrementId());
    }

    private String truncate(String str, int length) {
        if (str.length() <= length) {
            return str;
        } else {
            return str.substring(0, length);
        }
    }

    public SalesOrderContainer generateSalesOrderContainer(List<OrderItemInvoicing> orderItemInvoicings) throws IOException {
        log.info("order item invoicings to sales order container: {}", orderItemInvoicings);
        SalesOrderContainer salesOrderContainer = new SalesOrderContainer();
        List<SalesOrder> salesOrders = new ArrayList<>();
        Map<String, List<OrderItemInvoicing>> groupedInvoicings = orderItemInvoicings.stream()
                .collect(groupingBy(OrderItemInvoicing::getOrderIncrementId));
        for (Map.Entry<String, List<OrderItemInvoicing>> entry : groupedInvoicings.entrySet()) {
            List<OrderItemInvoicing> items = entry.getValue();
            SalesOrder salesOrder = new SalesOrder();
            salesOrder.setOrderType(items.get(0).getOrderType());
            salesOrder.setSalesOrg(items.get(0).getSalesOrg());
            salesOrder.setSalesOffice(items.get(0).getSalesOffice());
            salesOrder.setPoNumber(items.get(0).getOrderIncrementId());

            Party customer = orderItemInvoicingToParty(items.get(0), false);

            salesOrder.setBillingParty1(customer);
            salesOrder.setPayerParty(customer);
            salesOrder.setOrderParty(orderItemInvoicingToParty(items.get(0), true));

            List<LineItem> lineItems = new ArrayList<>();
            for (OrderItemInvoicing item : items) {
                LineItem lineItem = new LineItem();
                lineItem.setOrderItemId(item.getOrderItemId());
                lineItem.setMaterial(item.getMaterial());
                lineItem.setMaterialDescription(item.getMaterialDescription());
                lineItem.setQuantity(Integer.toString(item.getQuantity()));
                lineItem.setUnit(item.getUnit());
                lineItem.setNetPrice(item.getPriceNet());
                lineItems.add(lineItem);

                if (!productInvoicingAreEqual(salesOrder, item) || !partyAreEqual(customer, orderItemInvoicingToParty(item, false))) {
                    log.error("mismatching order item invoicings for an order: {}", items);
                    throw new Error("Mismatching line items for sales order");
                }
            }
            salesOrder.setLineItems(lineItems);

            salesOrder.setReference(Long.toString(incrementId.generateInvoicingIncrementId()));
            salesOrder.setBillingDate(LocalDate.now());
            salesOrders.add(salesOrder);
        }
        salesOrderContainer.setSalesOrders(salesOrders);
        return salesOrderContainer;
    }

    public void copyExportedDataToOrderItems(SalesOrderContainer salesOrderContainer) {
        for (SalesOrder salesOrder : salesOrderContainer.getSalesOrders()) {
            for (LineItem lineItem : salesOrder.getLineItems()) {
                OrderItem orderItem = orderItemRepository.findById(lineItem.getOrderItemId()).orElseThrow();
                orderItem.setInvoicingStatus(OrderItemInvoicingStatus.INVOICED);
                orderItem.setInvoicingIncrementId(salesOrder.getReference());
                orderItemRepository.save(orderItem);
            }
        }
    }

    public String getSalesOrderContainerFilename(SalesOrderContainer salesOrderContainer) {
        // assume senderId is constant and billingDate is today
        SalesOrder salesOrder = salesOrderContainer.getSalesOrders().get(0);
        String date = salesOrder.getBillingDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "MTIL_IN_" + salesOrder.getSenderId() + "_" + date + ".xml";
    }

    public String salesOrderContainerToXml(SalesOrderContainer container) throws JsonProcessingException {
        XmlMapper mapper = new XmlMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        mapper.registerModule(new JavaTimeModule());
        return mapper.writeValueAsString(container);
    }
}
