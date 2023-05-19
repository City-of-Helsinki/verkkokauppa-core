package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.accounting.RefundAccounting;
import fi.hel.verkkokauppa.order.repository.jpa.RefundAccountingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RefundAccountingService {

    private Logger log = LoggerFactory.getLogger(RefundAccountingService.class);

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
