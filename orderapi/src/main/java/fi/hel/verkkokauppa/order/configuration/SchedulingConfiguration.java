package fi.hel.verkkokauppa.order.configuration;

import fi.hel.verkkokauppa.order.service.accounting.AccountingSlipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfiguration {

    @Autowired
    private AccountingSlipService accountingSlipService;

    @Scheduled(cron = "0 15 0 * * *") // Nighty 0:15 o'clock
    public void scheduleCreateAccountingData() {
        accountingSlipService.createAccountingData();
    }

}
