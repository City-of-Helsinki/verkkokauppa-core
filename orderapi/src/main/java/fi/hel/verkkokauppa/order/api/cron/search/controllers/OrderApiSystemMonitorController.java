package fi.hel.verkkokauppa.order.api.cron.search.controllers;

import fi.hel.verkkokauppa.order.api.cron.search.SearchNotificationService;
import fi.hel.verkkokauppa.order.api.cron.search.dto.PaymentResultDto;
import fi.hel.verkkokauppa.order.api.cron.search.payment.SearchPaymentService;
import fi.hel.verkkokauppa.order.api.cron.search.renewal.SearchRenewalService;
import fi.hel.verkkokauppa.order.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class OrderApiSystemMonitorController {

    @Autowired
    private SearchRenewalService searchRenewalService;

    @Autowired
    private SearchPaymentService searchPaymentService;

    @Autowired
    private SearchNotificationService searchNotificationService;

    @PostMapping(value = "/cron/talpa-order-api-system-monitor",  produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> runOrderApiSystemMonitorChecks() {

        // search time is from 4:00 onwards
        ResponseEntity<Integer> renewalsResponse = findSuccessfullyPaidRenewals(4, 0);
        String renewalsReturnMessage;
        int renewalsCount = (renewalsResponse.getBody() != null) ? renewalsResponse.getBody() : 0;
        if (renewalsCount == 0) {
            renewalsReturnMessage = "ALERT: No successfully paid renewals found. Error notification sent.";
        } else {
            renewalsReturnMessage = "Found " + renewalsCount + " successfully paid renewal(s).";
        }

        return ResponseEntity.ok().body(
                renewalsReturnMessage + "\n\n" +
                "All talpa order-api system monitor checks have been executed."
        );

    }

    @Operation(description = "Finds successfully paid subscription renewals created today after given time. " +
            "The default time is 4:00 in the morning. If none are found, an error notification is sent.")
    @GetMapping(value = "/cron/find-successfully-paid-renewals", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Integer> findSuccessfullyPaidRenewals(
            @Parameter(description = "Find renewals created after (hours). " +
                    "Give hours without a leading zero. " +
                    "If left blank, the default value (4) is used.")
            @RequestParam(value = "hours", defaultValue = "4") int hours,

            @Parameter (description = "Find renewals created after (minutes). " +
                    "Give minutes without a leading zero. " +
                    "If left blank, the default value (0) is used. ")
            @RequestParam(value = "minutes", defaultValue = "0") int minutes
    ) {

        List<PaymentResultDto> dtos = new ArrayList<>();
        try {
            List<Order> subscriptionOrders = searchRenewalService.getRenewalsToday(hours, minutes);
            log.debug("Searching successfully paid renewals, renewals found: {} (payment status not known yet)", subscriptionOrders.size());

            List<String> orderIds = new ArrayList<>();
            for (Order order : subscriptionOrders) {
                orderIds.add(order.getOrderId());
            }

            dtos = searchPaymentService.findPaymentsByStatusAndOrderIds(orderIds, "payment_paid_online");
            log.debug("Searching successfully paid renewals, found: {}", dtos.size());

            if (dtos.isEmpty()) {
                searchNotificationService.sendNoSuccessfulRenewalPaymentsAlert(hours, minutes);
            }

        } catch (Exception e) {
            log.error("find-successfully-paid-renewals error", e);
        }
        return ResponseEntity.ok().body(dtos.size());
    }

}
