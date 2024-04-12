package fi.hel.verkkokauppa.order.service.accounting;

import com.jcraft.jsch.*;
import fi.hel.verkkokauppa.common.configuration.SAP;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class FileExportService {
    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Value("${ssh.knowhosts.path}")
    private String sshKnownHostsPath;

    @Autowired
    private SAP sap;

    public boolean isLocal() {
        return activeProfile != null && activeProfile.equals("local");
    }

    private ChannelSftp ConnectToChannelSftp(SAP.Interface i) {
        try {
            ChannelSftp channelSftp = setupJsch(i);
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

    private ChannelSftp setupJsch(SAP.Interface i) throws JSchException {
        JSch jsch = new JSch();

        log.debug("Connecting to server with username [{}]", sap.getUsername(i));
        Session jschSession = jsch.getSession(sap.getUsername(i), sap.getUrl(i));
        jschSession.setPassword(sap.getPassword(i));

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

    public void export(SAP.Interface i, String fileContent, String filename) {
        if (sap.getUrl(i) == null || sap.getUrl(i).isEmpty()) {
            log.debug("Not exporting file, server url not set");
            return;
        }

        ChannelSftp channelSftp = ConnectToChannelSftp(i);

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
}
