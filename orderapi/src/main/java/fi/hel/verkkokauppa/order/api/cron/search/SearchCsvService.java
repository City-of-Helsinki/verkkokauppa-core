package fi.hel.verkkokauppa.order.api.cron.search;


import fi.hel.verkkokauppa.order.api.cron.search.dto.PaymentResultDto;
import fi.hel.verkkokauppa.order.api.cron.search.dto.RefundResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SearchCsvService {

    public String generateCsvDataPayments(List<PaymentResultDto> unaccountedPayments) {
        StringBuilder csvBuilder = new StringBuilder("orderId,paymentId,paidAt,createdAt,paytrailTransactionId\n");

        unaccountedPayments.forEach(payment -> csvBuilder.append(String.format(
                "%s,%s,%s,%s,%s\n",
                payment.getOrderId(),
                payment.getPaymentId(),
                payment.getPaidAt() != null ? payment.getPaidAt().toString() : "N/A",
                payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : "N/A",
                payment.getPaytrailTransactionId() != null ? payment.getPaytrailTransactionId() : "N/A"
        )));
        log.info("Csv data: {}", csvBuilder);

        return csvBuilder.toString();
    }

    public String generateCsvDataRefunds(List<RefundResultDto> unaccountedRefunds) {
        StringBuilder csvBuilder = new StringBuilder("orderId,refundId,updatedAt,createdAt,paytrailTransactionId,paytrailMerchantId,namespace,total\n");

        unaccountedRefunds.forEach(refund -> csvBuilder.append(String.format(
                "%s,%s,%s,%s,%s,%s,%s,%s\n",
                refund.getOrderId(),
                refund.getRefundId(),
                refund.getUpdatedAt() != null ? refund.getUpdatedAt().toString() : "N/A",
                refund.getCreatedAt() != null ? refund.getCreatedAt().toString() : "N/A",
                refund.getRefundTransactionId() != null ? refund.getRefundTransactionId() : "N/A",
                refund.getPaytrailMerchantId() != null ? refund.getPaytrailMerchantId() : "N/A",
                refund.getNamespace() != null ? refund.getNamespace() : "N/A",
                refund.getTotal() != null ? refund.getTotal() : "N/A"
        )));
        log.info("Csv data: {}", csvBuilder);

        return csvBuilder.toString();
    }
}
