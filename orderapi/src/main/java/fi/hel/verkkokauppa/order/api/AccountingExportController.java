package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipDto;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingExportDataTransformer;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlip;
import fi.hel.verkkokauppa.order.service.accounting.AccountingExportDataService;
import fi.hel.verkkokauppa.order.service.accounting.AccountingExportService;
import fi.hel.verkkokauppa.order.service.accounting.AccountingSearchService;
import fi.hel.verkkokauppa.order.service.accounting.AccountingSlipService;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
public class AccountingExportController {

    private Logger log = LoggerFactory.getLogger(AccountingExportController.class);

    @Autowired
    private AccountingSlipService accountingSlipService;

    @Autowired
    private AccountingExportDataService accountingExportDataService;

    @Autowired
    private AccountingExportService accountingExportService;

    @Autowired
    private AccountingSearchService accountingSearchService;

    @PostMapping(value = "/accounting/export/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountingExportDataDto> generateAccountingExportData(@RequestParam(value = "accountingSlipId") String accountingSlipId) {
        try {
            AccountingSlip accountingSlip = accountingSlipService.getAccountingSlip(accountingSlipId);
            AccountingSlipDto accountingSlipWithRows = accountingSlipService.getAccountingSlipDtoWithRows(accountingSlip);
            AccountingExportDataDto exportData = accountingExportDataService.createAccountingExportDataDto(accountingSlipWithRows);

            return ResponseEntity.ok().body(exportData);
        } catch (Exception e) {
            log.error("generating accounting export data failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-generate-accounting-export-data", "failed to generate accounting export data")
            );
        }
    }

    @GetMapping(value = "/accounting/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountingExportDataDto>> ExportAccountingData() {
        List<AccountingExportDataDto> result = new ArrayList<>();

        try {
            List<AccountingExportData> accountingExportDataList = accountingSearchService.getNotExportedAccountingExportData();

            for (AccountingExportData accountingExportData : accountingExportDataList) {
                AccountingExportDataDto accountingExportDataDto = new AccountingExportDataTransformer().transformToDto(accountingExportData);

                accountingExportService.exportAccountingData(accountingExportDataDto);
                result.add(accountingExportDataDto);
            }

            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            log.error("exporting accounting data failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-export-accounting-data", "failed to export accounting data")
            );
        }
    }

    @GetMapping(value = "/accounting/export/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<LocalDate>> listAccountingExportDataIds() {
        try {
            List<LocalDate> accountingExportDataIds = accountingExportDataService.getAccountingExportDataTimestamps();

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
            List<AccountingExportData> exportDatas = accountingExportDataService.getAccountingExportDataByTimestamp(timestamp);
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
            AccountingExportData exportData = accountingExportDataService.getAccountingExportData(accountingSlipId);
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
