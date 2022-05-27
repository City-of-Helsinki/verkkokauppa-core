package fi.hel.verkkokauppa.order.service.accounting;

import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;
import com.jcraft.jsch.*;
import io.micrometer.core.instrument.util.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@RunWith(SpringJUnit4ClassRunner.class )
public class AccountingExportServiceTest {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final JSch JSCH = new JSch();
    private static final int TIMEOUT = 5000;

    @Rule
    public final FakeSftpServerRule sftpServer = new FakeSftpServerRule();

    private static Session connectToServer(FakeSftpServerRule sftpServer) throws JSchException {
        return connectToServerAtPort(sftpServer.getPort());
    }

    private static Session connectToServerAtPort(int port) throws JSchException {
        Session session = createSessionWithCredentials(USERNAME, PASSWORD, port);
        session.connect(TIMEOUT);
        return session;
    }

    private static Session createSessionWithCredentials(String username, String password, int port) throws JSchException {
        Session session = JSCH.getSession(username, "127.0.0.1", port);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setPassword(password);
        return session;
    }

    @Test
    public void whenUploadFileUsingJsch_thenSuccess() throws JSchException, SftpException, IOException {
        Session jschSession = connectToServer(sftpServer);
        ChannelSftp channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
        channelSftp.connect();

        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><test>Testing</test>";
        byte[] strToBytes = xml.getBytes(StandardCharsets.UTF_8);

        try (InputStream stream = new ByteArrayInputStream(strToBytes)) {
            channelSftp.put(stream, "testFile.xml");

            InputStream fileFromServer = channelSftp.get("testFile.xml");
            String fileContent = IOUtils.toString(fileFromServer, StandardCharsets.UTF_8);

            assertFalse(fileContent.isEmpty());
            assertEquals(xml, fileContent);

            channelSftp.disconnect();
            jschSession.disconnect();
        }
    }

    @Test
    public void whenXmlConvertedToFileInputStream_thenCorrect() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><test>Testing</test>";
        byte[] strToBytes = xml.getBytes();

        try (InputStream stream = new ByteArrayInputStream(strToBytes)) {
            String result = IOUtils.toString(stream, StandardCharsets.UTF_8);

            assertEquals(xml, result);
        }
    }

}

