package fi.hel.verkkokauppa.common.service;


import fi.hel.verkkokauppa.common.service.dto.CheckPaymentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SleepService {

    public void sleepWithRetry(long millis, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                Thread.sleep(millis);
                return; // success, exit
            } catch (InterruptedException e) {
                attempts++;
                log.error("Sleep interrupted (attempt {}/{}). Retrying...", attempts, maxRetries, e);
                // restore interrupted flag if last attempt
                if (attempts >= maxRetries) {
                    Thread.currentThread().interrupt();
                    log.warn("Max retries reached, restoring interrupt flag.");
                }
            }
        }
    }
}
