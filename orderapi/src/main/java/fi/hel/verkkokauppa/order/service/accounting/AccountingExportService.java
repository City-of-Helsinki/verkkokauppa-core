package fi.hel.verkkokauppa.order.service.accounting;

import com.jcraft.jsch.*;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipRowDto;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingExportDataTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingSlipTransformer;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlip;
import fi.hel.verkkokauppa.order.repository.jpa.AccountingExportDataRepository;
import fi.hel.verkkokauppa.order.service.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AccountingExportService {

    public static final int RUNNING_NUMBER_LENGTH = 4;

    private Logger log = LoggerFactory.getLogger(AccountingExportService.class);

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Value("${sap.sftp.server.url}")
    private String sftpServerUrl;

    @Value("${sap.sftp.server.username}")
    private String sftpServerUsername;

    @Value("${sap.sftp.server.password}")
    private String sftpServerPassword;

    @Value("${ssh.knowhosts.path}")
    private String sshKnownHostsPath;

    @Autowired
    private AccountingExportDataRepository exportDataRepository;

    @Autowired
    private AccountingSlipService accountingSlipService;

    @Autowired
    private AccountingExportDataService accountingExportDataService;

    @Autowired
    private ServiceUtils serviceUtils;


    public void exportAccountingData(AccountingExportDataDto exportData) {
        String accountingSlipId = exportData.getAccountingSlipId();
        AccountingSlip accountingSlip = accountingSlipService.getAccountingSlip(accountingSlipId);
        AccountingSlipDto accountingSlipDto = new AccountingSlipTransformer().transformToDto(accountingSlip);

        String senderId = accountingSlipDto.getSenderId();
        String filename = constructAccountingExportFileName(senderId, exportData.getTimestamp());

        export(exportData.getXml(), filename);

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

    public void export(String fileContent, String filename) {
        if (sftpServerUrl == null || sftpServerUrl.isEmpty()) {
            log.debug("Not exporting file, server url not set");
            return;
        }

        ChannelSftp channelSftp = ConnectToChannelSftp();

        byte[] strToBytes = fileContent.getBytes();

        try (InputStream stream = new ByteArrayInputStream(strToBytes)) {
            if (isLocal()) {
                // Local development moves files under share folder, normally it moves to home dir.
                filename = "share/" + filename;
            }
            channelSftp.put(stream, filename);
            channelSftp.disconnect();

            log.info("Exported file [" + filename + "] succesfully");
        } catch (SftpException | IOException e) {
            log.debug(e.getLocalizedMessage());
            log.debug("Failed to export accounting data");
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("export-accounting-data-failed", "Failed to export accounting data. Transfer failed")
            );
        }

    }

    private ChannelSftp ConnectToChannelSftp() {
        try {
            ChannelSftp channelSftp = setupJsch();
            channelSftp.connect();
            log.info("Connected to the sftp channel succesfully");

            return channelSftp;
        } catch (JSchException e) {
            log.debug("Failed to export accounting data. Connection to server failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("export-accounting-data-server-connection-failed",
                            "Failed to export accounting data. Connection to server failed")
            );
        }
    }

    private ChannelSftp setupJsch() throws JSchException {
        JSch jsch = new JSch();

        log.debug("Connecting to server with username [{}]", sftpServerUsername);
        Session jschSession = jsch.getSession(sftpServerUsername, sftpServerUrl);
        jschSession.setPassword(sftpServerPassword);

        jsch.setKnownHosts(sshKnownHostsPath);

        if (isLocal()) {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            jschSession.setConfig(config);
            jschSession.setPort(2222);
        }

        jschSession.connect();
        log.info("Connected to the server succesfully");

        return (ChannelSftp) jschSession.openChannel("sftp");
    }

    public boolean isLocal() {
        return activeProfile != null && activeProfile.equals("local");
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
        Set<String> balanceProfitCenters = serviceUtils.combineKeySets(
                ordersByBalanceProfitCenter.keySet(),
                refundsByBalanceProfitCenter.keySet(),
                String.class
        );

        // collect all rows by balance profit center
        List<AccountingSlipRowDto> newRows = new ArrayList<>();
//        for (Map.Entry<String, List<AccountingSlipRowDto>> orderedAccountings : ordersByBalanceProfitCenter.entrySet()) {
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
            if (row.getRowType().contains("refund")) {
                refundRows.add(row);
            } else {
                orderRows.add(row);
            }

        }
    }

}
