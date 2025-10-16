package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.configuration.SAP;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.order.api.data.invoice.OrderItemInvoicingDto;
import fi.hel.verkkokauppa.order.api.data.invoice.xml.SalesOrderContainer;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicing;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import fi.hel.verkkokauppa.order.service.accounting.FileExportService;
import fi.hel.verkkokauppa.order.service.invoice.InvoiceXmlService;
import fi.hel.verkkokauppa.order.service.invoice.InvoicingExportService;
import fi.hel.verkkokauppa.order.service.invoice.OrderItemInvoicingService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class OrderInvoicingController {
    private Logger log = LoggerFactory.getLogger(OrderInvoicingController.class);

    @Autowired
    private OrderItemInvoicingService orderItemInvoicingService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private InvoicingExportService invoicingExportService;

    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    private QueueConfigurations queueConfigurations;

    @Autowired
    private FileExportService fileExportService;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @Autowired
    private InvoiceXmlService invoiceXmlService;

    @PostMapping(value = "/order/invoicing/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderItemInvoicingDto>> createOrderInvoicing(@RequestBody List<OrderItemInvoicingDto> dtos) {
        List<OrderItemInvoicingDto> res = new ArrayList<>();
        for (OrderItemInvoicingDto dto : dtos) {
            res.add(this.orderItemInvoicingService.create(dto));
            orderItemService.setInvoicingStatus(dto.getOrderItemId(), OrderItemInvoicingStatus.CREATED);
        }
        return ResponseEntity.ok().body(res);
    }

    @PostMapping(value = "/invoicing/export")
    public ResponseEntity<Void> exportInvoicings() {
        try {
            List<OrderItemInvoicing> orderItemInvoicings = orderItemInvoicingService.findInvoicingsToExport();
            orderItemInvoicings = orderItemInvoicingService.filterAndUpdateCancelledOrders(orderItemInvoicings);
            if (orderItemInvoicings.isEmpty()) {
                return ResponseEntity.ok().build();
            }
            SalesOrderContainer salesOrderContainer = invoicingExportService.generateSalesOrderContainer(orderItemInvoicings);
            String xml = invoicingExportService.salesOrderContainerToXml(salesOrderContainer);

            String xmlFileName = invoicingExportService.getSalesOrderContainerFilename(salesOrderContainer);
            // save xml to elasticsearch
            invoiceXmlService.save(xmlFileName,xml);

            log.info(xml);
            fileExportService.export(SAP.Interface.INVOICING, xml, xmlFileName);
            invoicingExportService.copyExportedDataToOrderItems(salesOrderContainer);
            orderItemInvoicingService.markInvoicingsInvoiced(orderItemInvoicings);
            JSONObject invoicedEmail = invoicingExportService.generateInvoicedEmail(salesOrderContainer);
            invoicingExportService.sendInvoicedEmail(invoicedEmail);
            saveHistoryService.saveInvoicedEmailHistory(invoicedEmail);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("failed to export invoicings", e);
            sendNotificationService.sendErrorNotification(
                    "Endpoint: /invoicing/export. Failed to export invoicings to SAP",
                    e
            );
            return ResponseEntity.internalServerError().build();
        }
    }
}
