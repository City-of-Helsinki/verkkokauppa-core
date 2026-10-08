package fi.hel.verkkokauppa.order.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.configuration.SAP;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.order.api.data.invoice.OrderItemInvoicingDto;
import fi.hel.verkkokauppa.order.api.data.invoice.xml.SalesOrderContainer;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicing;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import fi.hel.verkkokauppa.order.service.accounting.FileExportService;
import fi.hel.verkkokauppa.order.service.invoice.InvoiceXmlService;
import fi.hel.verkkokauppa.order.service.invoice.InvoicingExportService;
import fi.hel.verkkokauppa.order.service.invoice.OrderItemInvoicingService;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import fi.hel.verkkokauppa.order.service.voucher.VoucherService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class VoucherController {
    private Logger log = LoggerFactory.getLogger(VoucherController.class);

    @Autowired
    private OrderItemInvoicingService orderItemInvoicingService;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @Autowired
    private VoucherService voucherService;


    @PostMapping(value = "/order/voucher/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderItem>> voucherCheckAndTokenCharge(@RequestParam(value = "merchantId") String merchantId, @RequestParam(value = "namespace") String namespace, @RequestBody List<OrderItem> items) throws JsonProcessingException {


        List<OrderItem> res = voucherService.voucherQRCodeCheck(merchantId, namespace, items);
        return ResponseEntity.ok().body(res);
    }

}
