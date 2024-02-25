package fi.hel.verkkokauppa.common.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SAP {
    public enum Interface { ACCOUNTING, INVOICING }

    @Autowired
    private Environment env;

    public String getUrl(Interface i) {
        if (i.equals(Interface.ACCOUNTING)) {
            return env.getProperty("sap.sftp.server.url");
        }
        if (i.equals(Interface.INVOICING)) {
            return env.getProperty("sap.sftp.server.invoicing.url");
        }
        return null;
    }

    public String getUsername(Interface i) {
        if (i.equals(Interface.ACCOUNTING)) {
            return env.getProperty("sap.sftp.server.username");
        }
        if (i.equals(Interface.INVOICING)) {
            return env.getProperty("sap.sftp.server.invoicing.username");
        }
        return null;
    }

    public String getPassword(Interface i) {
        if (i.equals(Interface.ACCOUNTING)) {
            return env.getProperty("sap.sftp.server.password");
        }
        if (i.equals(Interface.INVOICING)) {
            return env.getProperty("sap.sftp.server.invoicing.password");
        }
        return null;
    }
}
