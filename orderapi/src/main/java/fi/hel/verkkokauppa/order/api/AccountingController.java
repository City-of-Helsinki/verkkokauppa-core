package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipDto;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlip;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlipRow;
import fi.hel.verkkokauppa.order.service.accounting.AccountingSlipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AccountingController {

    private Logger log = LoggerFactory.getLogger(AccountingController.class);

    @Autowired
    private AccountingSlipService accountingSlipService;

    @GetMapping(value = "/accounting/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountingSlipDto>> createAccountingData() {
        try {
            List<AccountingSlipDto> accountingSlips = accountingSlipService.createAccountingData();

            return ResponseEntity.ok().body(accountingSlips);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("creating accounting data failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-accounting-data", "failed to create accounting data")
            );
        }
    }

    @GetMapping(value = "/accounting/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountingSlipDto> getAccounting(@RequestParam(value = "accountingSlipId") String accountingSlipId) {
        AccountingSlip accounting;
        List<AccountingSlipRow> accountingSlipRows;

        try {
            accounting = accountingSlipService.getAccountingSlip(accountingSlipId);
            accountingSlipRows = accountingSlipService.getAccountingSlipRows(accountingSlipId);

            AccountingSlipDto accountingSlipDto = accountingSlipService.transformAccountingSlipWithRowsToDto(accounting, accountingSlipRows);

            return ResponseEntity.ok().body(accountingSlipDto);

        } catch (CommonApiException cae) {
                throw cae;
        } catch (Exception e) {
            log.error("getting accounting data failed, accountingSlipId: " + accountingSlipId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-accounting-data", "failed to get accounting data with accountingSlipId [" + accountingSlipId + "]")
            );
        }
    }

}
