package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipDto;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingExportDataTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingSlipTransformer;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlip;
import fi.hel.verkkokauppa.order.service.accounting.AccountingExportService;
import fi.hel.verkkokauppa.order.service.accounting.AccountingSlipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class AccountingExportController {

    private Logger log = LoggerFactory.getLogger(AccountingExportController.class);

    @Autowired
    private AccountingSlipService accountingSlipService;

    @Autowired
    private AccountingExportService accountingExportService;

    @PostMapping(value = "/accounting/export/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountingExportDataDto> generateAccountingExportData(@RequestParam(value = "accountingSlipId") String accountingSlipId) {
        try {
            AccountingSlip accountingSlip = accountingSlipService.getAccountingSlip(accountingSlipId);
            AccountingSlipDto accountingSlipWithRows = accountingSlipService.getAccountingSlipDtoWithRows(accountingSlip);
            AccountingExportDataDto exportData = accountingExportService.createAccountingExportDataDto(accountingSlipWithRows);

            return ResponseEntity.ok().body(exportData);
        } catch (Exception e) {
            log.error("generating accounting export data failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-generate-accounting-export-data", "failed to generate accounting export data")
            );
        }
    }

    @GetMapping(value = "/accounting/export/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> listAccountingExportDataIds() {
        try {
            List<String> accountingExportDataIds = accountingExportService.getAccountingExportDataTimestamps();

            return ResponseEntity.ok().body(accountingExportDataIds);
        } catch (Exception e) {
            log.error("listing accounting export data failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-list-accounting-export-data", "failed to list accounting export data")
            );
        }
    }

    @GetMapping(value = "/accounting/export/getByTimestamp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountingExportDataDto>> getAccountExportDataWithTimestamp(@RequestParam(value = "timestamp") String timestamp) {
        try {
            List<AccountingExportData> exportDatas = accountingExportService.getAccountingExportDataByTimestamp(timestamp);
            List<AccountingExportDataDto> result = new ArrayList<>();
            exportDatas.forEach(exportData -> result.add(new AccountingExportDataTransformer().transformToDto(exportData)));

            return ResponseEntity.ok().body(result);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("failed to get accounting export data with id [" + timestamp + "]", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-accounting-export-data", "failed to get accounting export data with id [" + timestamp + "]")
            );
        }
    }

    @GetMapping(value = "/accounting/export/getByAccountingSlipId", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountingExportDataDto> listAccountingExportData(@RequestParam(value = "accountingSlipId") String accountingSlipId) {
        try {
            AccountingExportData exportData = accountingExportService.getAccountingExportData(accountingSlipId);
            AccountingExportDataDto dto = new AccountingExportDataTransformer().transformToDto(exportData);

            return ResponseEntity.ok().body(dto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("failed to get accounting export data with accounting slip id [" + accountingSlipId + "]", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-accounting-export-data",
                            "failed to get accounting export data with accounting slip id [" + accountingSlipId + "]")
            );
        }
    }

}
