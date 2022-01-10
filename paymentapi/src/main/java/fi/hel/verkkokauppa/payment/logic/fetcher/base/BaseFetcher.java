package fi.hel.verkkokauppa.payment.logic.fetcher.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.payment.logic.visma.VismaAuth;
import lombok.Getter;
import org.helsinki.vismapay.response.VismaPayResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Getter
public class BaseFetcher {
    ObjectMapper mapper = new ObjectMapper();

    protected boolean isSuccessResponse(VismaPayResponse response) {
        return response.getResult() != null && response.getResult() == 0;
    }

    public static String getMethodName() {
        try {
            return Thread.currentThread().getStackTrace()[2].getMethodName();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
