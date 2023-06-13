package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.accounting.RefundAccounting;
import fi.hel.verkkokauppa.order.repository.jpa.RefundAccountingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RefundAccountingService {

    @Autowired
    private RefundAccountingRepository refundAccountingRepository;

    public List<RefundAccounting> getRefundAccountings(List<String> refundIds) {
        List<RefundAccounting> accountings = new ArrayList<>();

        for (String refundId : refundIds) {
            RefundAccounting accounting = refundAccountingRepository.findByRefundId(refundId);

            if (accounting != null) {
                accountings.add(accounting);
            }
        }

        log.debug("refund accountings not found, refundIds: " + refundIds);
        return accountings;
    }

}
