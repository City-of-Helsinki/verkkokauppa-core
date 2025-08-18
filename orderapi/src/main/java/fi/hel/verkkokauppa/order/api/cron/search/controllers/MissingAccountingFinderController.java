package fi.hel.verkkokauppa.order.api.cron.search.controllers;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.order.api.cron.experience.ExperienceApiAccountingService;
import fi.hel.verkkokauppa.order.api.cron.search.SearchCsvService;
import fi.hel.verkkokauppa.order.api.cron.search.SearchNotificationService;
import fi.hel.verkkokauppa.order.api.cron.search.payment.SearchUnAccountedPayments;
import fi.hel.verkkokauppa.order.api.cron.search.dto.PaymentResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ExperienceApiAccountingService experienceApiAccountingService;



    @GetMapping(value = "/accounting/cron/find-missing-accounting", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PaymentResultDto>> findMissingAccountingsBasedOnPaymentPaid(
            @RequestParam(value = "createdAfter", required = false) String createdAfter,
            @RequestParam(value = "createAccountingAfter", required = false) String createAccountingAfter
    ) {
        try {
            List<PaymentResultDto> failedToAccount;

            // Define date-time formatter
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            // Parse the parameters if they are provided
            LocalDateTime createdAfterDateTime = createdAfter != null ? LocalDateTime.parse(createdAfter, formatter) : null;
            LocalDateTime createAccountingAfterDateTime = createAccountingAfter != null ? LocalDateTime.parse(createAccountingAfter, formatter) : null;


            if (createdAfterDateTime != null) {
                failedToAccount = this.searchUnAccountedPayments.findUnaccountedPayments(
                        createdAfterDateTime
                );
            } else {
                // Default case find all unaccounted
                failedToAccount = this.searchUnAccountedPayments.findUnaccountedPayments();
            }


            if (failedToAccount.isEmpty()) {
                log.info("All orders and payments are accounted for.");
                return ResponseEntity.ok().body(failedToAccount);
            }

            // Notification can have all the
            String csvData = searchCsvService.generateCsvDataPayments(failedToAccount);
            searchNotificationService.sendUnaccountedPaymentsAlert(failedToAccount.size(), csvData);

            if (createAccountingAfterDateTime != null) {
                log.info("createAccountingAfterDateTime was {}", createAccountingAfter);
                List<PaymentResultDto> failedToAccountAfterDate = this
                        .searchUnAccountedPayments
                        .findUnaccountedPayments(
                                createAccountingAfterDateTime
                        );

                if (failedToAccountAfterDate.isEmpty()) {
                    log.info("failedToAccountAfterDate was empty returning failedToAccount count.");
                    return ResponseEntity.ok().body(failedToAccount);
                }

                // Sends create accounting to experience api
                this.experienceApiAccountingService.sendCreateAccountingRequests(failedToAccountAfterDate);
            }

            return ResponseEntity.ok().body(failedToAccount);

        } catch (CommonApiException cae) {
            log.error("Failed to find missing accounting data cae", cae);
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