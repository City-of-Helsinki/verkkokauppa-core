package fi.hel.verkkokauppa.payment.api.cron;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.payment.api.cron.dto.SynchronizeResultDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;

@RestController
@Slf4j
public class PaytrailStatusCheckController {

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private OnlinePaymentService onlinePaymentService;


    @GetMapping(value = "/synchronize-paytrail-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SynchronizeResultDto>> checkAndSynchronizePaytrailStatus(
            @RequestParam(value = "createdAfter", required = false) String createdAfter,
            @RequestParam(value = "createdBefore", required = false) String createdBefore
    ) {
        LocalDateTime createdAfterDateTime = null;
        LocalDateTime createdBeforeDateTime = null;
        LocalDateTime startDate = LocalDateTime.now();
        // Define date-time formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        // Parse the parameters if they are provided
        if( createdAfter != null )
        {
            createdAfterDateTime = LocalDateTime.parse(createdAfter, formatter);
        } else {
            // startdatetime not provided, check for today
            createdAfterDateTime = startDate.with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay());
        }

        if( createdBefore != null )
        {
            createdBeforeDateTime = LocalDateTime.parse(createdBefore, formatter);
        } else {
            // enddatetime not provided, check for today
            createdBeforeDateTime = startDate.minusMinutes(5);
        }
        log.info("Synchronizing payments between {} and {}", createdAfterDateTime, createdBeforeDateTime);

        try {
            // get list of payments to synchronize
            List<Payment> payments = onlinePaymentService.getUnpaidPaymentsToCheck(createdAfterDateTime, createdAfterDateTime);

            // get paytrail statuses for payments

            // update payments with new statuses

            return ResponseEntity.ok().body(null);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Failed to synchronize paytrail status", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-synchronize-paytrail-status", "Failed to synchronize paytrail status")
            );
        }
    }

}