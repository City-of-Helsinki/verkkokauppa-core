package fi.hel.verkkokauppa.order.service.accounting;

import com.jcraft.jsch.*;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingSlipDto;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingExportDataTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.AccountingSlipTransformer;
import fi.hel.verkkokauppa.order.model.accounting.AccountingSlip;
import fi.hel.verkkokauppa.order.repository.jpa.AccountingExportDataRepository;
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

@Service
public class AccountingExportService {

    public static final int RUNNING_NUMBER_LENGTH = 4;

    private Logger log = LoggerFactory.getLogger(AccountingExportService.class);

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


    public void exportAccountingData(AccountingExportDataDto exportData) {
        String accountingSlipId = exportData.getAccountingSlipId();
        AccountingSlip accountingSlip = accountingSlipService.getAccountingSlip(accountingSlipId);
        AccountingSlipDto accountingSlipDto = new AccountingSlipTransformer().transformToDto(accountingSlip);

        String senderId = accountingSlipDto.getSenderId();
        String timestamp = exportData.getTimestamp();
        String filename = constructAccountingExportFileName(senderId, timestamp);

        export(exportData.getXml(), filename);

        markAsExported(exportData);
    }

    public String constructAccountingExportFileName(String senderId, String ExportDataTimestamp) {
        LocalDate localDate = LocalDate.parse(ExportDataTimestamp);
        int year = localDate.getYear();
        int count = exportDataRepository.countAllByExportedStartsWith(Integer.toString(year));

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
            channelSftp.put(stream, filename);
            channelSftp.disconnect();

            log.info("Exported file [" + filename + "] succesfully");
        } catch (SftpException | IOException e) {
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

        jschSession.connect();
        log.info("Connected to the server succesfully");

        return (ChannelSftp) jschSession.openChannel("sftp");
    }

    private void markAsExported(AccountingExportDataDto exportData) {
        exportData.setExported(DateTimeUtil.getDate());

        exportDataRepository.save(new AccountingExportDataTransformer().transformToEntity(exportData));
        log.debug("marked accounting exported, accounting slip id: " + exportData.getAccountingSlipId());
    }

}
