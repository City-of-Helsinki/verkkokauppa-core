package fi.hel.verkkokauppa.order.api.cron.search.controllers;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.order.api.cron.experience.ExperienceApiRefundAccountingService;
import fi.hel.verkkokauppa.order.api.cron.search.SearchCsvService;
import fi.hel.verkkokauppa.order.api.cron.search.SearchNotificationService;
import fi.hel.verkkokauppa.order.api.cron.search.dto.RefundResultDto;
import fi.hel.verkkokauppa.order.api.cron.search.refund.SearchUnAccountedRefunds;
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
public class MissingRefundAccountingFinderController {

    @Autowired
    private SearchCsvService searchCsvService;

    @Autowired
    private SearchNotificationService searchNotificationService;

    @Autowired
    private SearchUnAccountedRefunds searchUnAccountedRefunds;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ExperienceApiRefundAccountingService experienceApiRefundAccountingService;



    @GetMapping(value = "/accounting/cron/find-missing-refund-accounting", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RefundResultDto>> findMissingAccountingsBasedOnRefundPaid(
            @RequestParam(value = "createdAfter", required = false) String createdAfter,
            @RequestParam(value = "createAccountingAfter", required = false) String createAccountingAfter
    ) {
        try {
            List<RefundResultDto> failedToAccount;

            // Define date-time formatter
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            // Parse the parameters if they are provided
            LocalDateTime createdAfterDateTime = createdAfter != null ? LocalDateTime.parse(createdAfter, formatter) : null;
            LocalDateTime createAccountingAfterDateTime = createAccountingAfter != null ? LocalDateTime.parse(createAccountingAfter, formatter) : null;


            if (createdAfterDateTime != null) {
                this.searchUnAccountedRefunds.findCreatedRefundsAndUpdateStatusFromPaytrail(createdAfterDateTime);
                failedToAccount = this.searchUnAccountedRefunds.findUnaccountedRefunds(
                        createdAfterDateTime
                );
            } else {
                this.searchUnAccountedRefunds.findCreatedRefundsAndUpdateStatusFromPaytrail();
                // Default case find all unaccounted
                failedToAccount = this.searchUnAccountedRefunds.findUnaccountedRefunds();
            }

            if (failedToAccount.isEmpty()) {
                log.info("All orders and payments are accounted for.");
                return ResponseEntity.ok().body(failedToAccount);
            }

            // Notification can have all the
            String csvData = searchCsvService.generateCsvDataRefunds(failedToAccount);
            searchNotificationService.sendUnaccountedRefundsAlert(failedToAccount.size(), csvData);

            if (createAccountingAfterDateTime != null) {
                log.info("createAccountingAfterDateTime was {}", createAccountingAfter);
                List<RefundResultDto> failedToAccountAfterDate = this
                        .searchUnAccountedRefunds
                        .findUnaccountedRefunds(
                                createAccountingAfterDateTime
                        );

                if (failedToAccountAfterDate.isEmpty()) {
                    log.info("failedToAccountAfterDate was empty returning failedToAccount count.");
                    return ResponseEntity.ok().body(failedToAccount);
                }

                // Sends create accounting to experience api
                this.experienceApiRefundAccountingService.sendCreateRefundAccountingRequests(failedToAccountAfterDate);
            }

            return ResponseEntity.ok().body(failedToAccount);

        } catch (CommonApiException cae) {
            log.error("Failed to find missing refund accounting data cae", cae);
            throw cae;
        } catch (Exception e) {
            log.error("Failed to find missing refund accounting data", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-find-missing-refund-accounting-data", "Failed to find missing refund accounting data")
            );
        }
    }

    @GetMapping(value = "/accounting/find-missing-refund-accounting", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RefundResultDto>> findMissingAccountingsBasedOnPaymentPaidNoEmail() {
        try {
            List<RefundResultDto> failedToAccount = this.searchUnAccountedRefunds.findUnaccountedRefunds();

            if (failedToAccount.isEmpty()) {
                log.info("All orders and refunds are accounted for.");
                return ResponseEntity.ok().body(failedToAccount);
            }

            return ResponseEntity.ok().body(failedToAccount);

        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("Failed to find missing refund accounting data", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-find-missing-refund-accounting-data", "Failed to find missing refund accounting data")
            );
        }
    }

}