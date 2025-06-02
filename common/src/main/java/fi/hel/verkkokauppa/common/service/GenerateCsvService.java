package fi.hel.verkkokauppa.common.service;


import fi.hel.verkkokauppa.common.service.dto.CheckPaymentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GenerateCsvService {

    public String generateCsvData(List<CheckPaymentDto> unaccountedPayments) {
        StringBuilder csvBuilder = new StringBuilder("orderId,paymentId,namespace,paytrailMerchantId,paidAt,createdAt,paytrailTransactionId,paymentProviderStatus\n");

        unaccountedPayments.forEach(payment -> csvBuilder.append(String.format(
                "%s,%s,%s,%s,%s,%s,%s,%s\n",
                payment.getOrderId(),
                payment.getPaymentId(),
                payment.getNamespace(),
                payment.getPaytrailMerchantId() != null ? payment.getPaytrailMerchantId() : "N/A",
                payment.getPaidAt() != null ? payment.getPaidAt().toString() : "N/A",
                payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : "N/A",
                payment.getPaytrailTransactionId() != null ? payment.getPaytrailTransactionId() : "N/A",
                payment.getPaymentProviderStatus() != null ? payment.getPaymentProviderStatus() : "N/A"
        )));
        log.info("Csv data: {}", csvBuilder);

        return csvBuilder.toString();
    }
}
