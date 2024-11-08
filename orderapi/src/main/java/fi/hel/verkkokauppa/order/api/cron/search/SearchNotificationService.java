package fi.hel.verkkokauppa.order.api.cron.search;

import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SearchNotificationService {

    @Autowired
    private SendNotificationService sendNotificationService;

    public void sendUnaccountedPaymentsAlert(int unaccountedCount, String csvData) {
        String message = String.format(
                "Alert: %d paid order(s) have no corresponding accounting entries. "
                        + "This may indicate an issue in the order processing system. "
                        + "Attached are the unaccounted payment details in CSV format.\n\n"
                        + "Unaccounted Payment Details:\n%s",
                unaccountedCount,
                csvData
        );

        sendNotificationService.sendErrorNotification(
                String.format("Unaccounted Paid Orders Alert: %d unaccounted orders", unaccountedCount),
                message
        );
    }
}
