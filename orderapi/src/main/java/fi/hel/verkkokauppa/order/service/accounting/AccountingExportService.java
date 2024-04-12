package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.configuration.SAP;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipRowDto;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingExportDataTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingSlipTransformer;
import fi.hel.verkkokauppa.order.constants.AccountingRowTypeEnum;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlip;
import fi.hel.verkkokauppa.order.repository.jpa.AccountingExportDataRepository;
import fi.hel.verkkokauppa.common.util.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AccountingExportService {

    public static final int RUNNING_NUMBER_LENGTH = 4;

    private Logger log = LoggerFactory.getLogger(AccountingExportService.class);

    @Autowired
    private AccountingExportDataRepository exportDataRepository;

    @Autowired
    private AccountingSlipService accountingSlipService;

    @Autowired
    private AccountingExportDataService accountingExportDataService;

    @Autowired
    private FileExportService fileExportService;

    public void exportAccountingData(AccountingExportDataDto exportData) {
        String accountingSlipId = exportData.getAccountingSlipId();
        AccountingSlip accountingSlip = accountingSlipService.getAccountingSlip(accountingSlipId);
        AccountingSlipDto accountingSlipDto = new AccountingSlipTransformer().transformToDto(accountingSlip);

        String senderId = accountingSlipDto.getSenderId();
        String filename = constructAccountingExportFileName(senderId, exportData.getTimestamp());

        fileExportService.export(SAP.Interface.ACCOUNTING, exportData.getXml(), filename);

        markAsExported(exportData);
    }

    public String constructAccountingExportFileName(String senderId, LocalDate exportDataTimestamp) {
        LocalDate startOfYear = LocalDate.of(exportDataTimestamp.getYear(), 1, 1);
        LocalDate endOfYear = LocalDate.of(exportDataTimestamp.getYear(), 12, 31);
        int count = exportDataRepository.countAllByExportedGreaterThanEqualAndExportedLessThanEqual(startOfYear, endOfYear);

        int runningNumber = count + 1;
        String runningNumberFormatted = String.format("%1$" + RUNNING_NUMBER_LENGTH + "s", runningNumber).replace(' ', '0');

        return "KP_IN_" + senderId + "_" + runningNumberFormatted + ".xml";
    }

    private void markAsExported(AccountingExportDataDto exportData) {
        exportData.setExported(DateTimeUtil.getFormattedDateTime().toLocalDate());

        exportDataRepository.save(new AccountingExportDataTransformer().transformToEntity(exportData));
        log.debug("marked accounting exported, accounting slip id: " + exportData.getAccountingSlipId());
    }

    public AccountingSlipDto createAccountingData(AccountingSlipDto accountingSlipWithRows) {
        List<AccountingSlipRowDto> refundRows = new ArrayList<>();
        List<AccountingSlipRowDto> orderRows = new ArrayList<>();

        splitAccountingSlipRowsToOrdersAndRefunds(
                accountingSlipWithRows.getRows(),
                orderRows,
                refundRows);

        // order each list by balanceProfitCenter
        Map<String, List<AccountingSlipRowDto>> ordersByBalanceProfitCenter;
        ordersByBalanceProfitCenter = accountingSlipService.groupAccountingSlipRowsByBalanceProfitCenter(orderRows);
        Map<String, List<AccountingSlipRowDto>> refundsByBalanceProfitCenter;
        refundsByBalanceProfitCenter = accountingSlipService.groupAccountingSlipRowsByBalanceProfitCenter(refundRows);

        // build set of all balanceProfitCenters
        Set<String> balanceProfitCenters = ServiceUtils.combineKeySets(
                ordersByBalanceProfitCenter.keySet(),
                refundsByBalanceProfitCenter.keySet(),
                String.class
        );

        // collect all rows by balance profit center
        List<AccountingSlipRowDto> newRows = new ArrayList<>();

        for (String balanceProfitCenter : balanceProfitCenters) {
            List<AccountingSlipRowDto> originalOrderRows = ordersByBalanceProfitCenter.get(balanceProfitCenter);
            List<AccountingSlipRowDto> originalRefundRows = refundsByBalanceProfitCenter.get(balanceProfitCenter);

            List<AccountingSlipRowDto> separatedRows = new ArrayList<>();
            if (originalOrderRows != null && !originalOrderRows.isEmpty()) {
                separatedRows = accountingExportDataService.separateVatRows(originalOrderRows, separatedRows);
            }
            if (originalRefundRows != null && !originalRefundRows.isEmpty()) {
                separatedRows = accountingExportDataService.separateVatRows(originalRefundRows, separatedRows);
            }

            // modify sum rows for XML
            separatedRows.forEach(row -> {
                if (row.getAccountingSlipRowId() != null) {
                    // set amountInDocumentCurrency to base amount for sum rows (for XML)
                    row.setAmountInDocumentCurrency(row.getBaseAmount());
                    // set base amount for sum rows to null (for XML)
                    row.setBaseAmount(null);
                }
            });

            if (originalOrderRows != null && !originalOrderRows.isEmpty()) {
                accountingExportDataService.addOrderIncomeEntryRow(originalOrderRows, separatedRows, accountingSlipWithRows.getHeaderText());
            }
            if (originalRefundRows != null && !originalRefundRows.isEmpty()) {
                accountingExportDataService.addRefundIncomeEntryRow(originalRefundRows, separatedRows, accountingSlipWithRows.getHeaderText());
            }

            newRows.addAll(separatedRows);
        }
        accountingSlipWithRows.setRows(newRows);

        return accountingSlipWithRows;
    }

    private void splitAccountingSlipRowsToOrdersAndRefunds(
            List<AccountingSlipRowDto> allRows,
            List<AccountingSlipRowDto> orderRows,
            List<AccountingSlipRowDto> refundRows) {

        for (AccountingSlipRowDto row : allRows) {
            if (row.getRowType() != null && row.getRowType().equals(AccountingRowTypeEnum.REFUND)) {
                refundRows.add(row);
            } else {
                orderRows.add(row);
            }

        }
    }

}
