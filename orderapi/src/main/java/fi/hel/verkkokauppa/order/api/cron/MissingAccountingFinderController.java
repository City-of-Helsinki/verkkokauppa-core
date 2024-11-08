package fi.hel.verkkokauppa.order.api.cron;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.api.cron.search.SearchCsvService;
import fi.hel.verkkokauppa.order.api.cron.search.SearchNotificationService;
import fi.hel.verkkokauppa.order.api.cron.search.SearchUnAccountedPayments;
import fi.hel.verkkokauppa.order.api.cron.search.dto.PaymentResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class MissingAccountingFinderController {

    @Autowired
    private SearchCsvService searchCsvService;

    @Autowired
    private SearchNotificationService searchNotificationService;

    @Autowired
    private SearchUnAccountedPayments searchUnAccountedPayments;

    @GetMapping(value = "/accounting/cron/find-missing-accounting", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PaymentResultDto>> findMissingAccountingsBasedOnPaymentPaid() {
        try {
            List<PaymentResultDto> failedToAccount = this.searchUnAccountedPayments.findUnaccountedPayments();

            if (failedToAccount.isEmpty()) {
                log.info("All orders and payments are accounted for.");
                return ResponseEntity.ok().body(failedToAccount);
            }

            String csvData = searchCsvService.generateCsvData(failedToAccount);
            searchNotificationService.sendUnaccountedPaymentsAlert(failedToAccount.size(), csvData);

            return ResponseEntity.ok().body(failedToAccount);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Failed to find missing accounting data", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-find-missing-accounting-data", "Failed to find missing accounting data")
            );
        }
    }

    @GetMapping(value = "/accounting/find-missing-accounting", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PaymentResultDto>> findMissingAccountingsBasedOnPaymentPaidNoEmail() {
        try {
            List<PaymentResultDto> failedToAccount = this.searchUnAccountedPayments.findUnaccountedPayments();

            if (failedToAccount.isEmpty()) {
                log.info("All orders and payments are accounted for.");
                return ResponseEntity.ok().body(failedToAccount);
            }

            return ResponseEntity.ok().body(failedToAccount);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Failed to find missing accounting data", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-find-missing-accounting-data", "Failed to find missing accounting data")
            );
        }
    }

}