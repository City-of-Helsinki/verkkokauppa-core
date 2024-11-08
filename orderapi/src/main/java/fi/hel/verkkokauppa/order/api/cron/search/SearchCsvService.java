package fi.hel.verkkokauppa.order.api.cron.search;


import fi.hel.verkkokauppa.order.api.cron.search.dto.PaymentResultDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchCsvService {

    public String generateCsvData(List<PaymentResultDto> unaccountedPayments) {
        StringBuilder csvBuilder = new StringBuilder("orderId,paymentId,paidAt,createdAt,paytrailTransactionId\n");

        unaccountedPayments.forEach(payment -> csvBuilder.append(String.format(
                "%s,%s,%s,%s,%s\n",
                payment.getOrderId(),
                payment.getPaymentId(),
                payment.getPaidAt() != null ? payment.getPaidAt().toString() : "N/A",
                payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : "N/A",
                payment.getPaytrailTransactionId() != null ? payment.getPaytrailTransactionId() : "N/A"
        )));

        return csvBuilder.toString();
    }
}
