package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.accounting.RefundItemAccounting;
import fi.hel.verkkokauppa.order.repository.jpa.RefundItemAccountingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RefundItemAccountingService {

    private Logger log = LoggerFactory.getLogger(RefundItemAccountingService.class);

    @Autowired
    private RefundItemAccountingRepository refundItemAccountingRepository;

    public List<RefundItemAccounting> getRefundItemAccountings(String refundId) {
        List<RefundItemAccounting> accountings = refundItemAccountingRepository.findByRefundId(refundId);

        if (accountings.size() > 0)
            return accountings;

        log.debug("refundItems not found, refundId: " + refundId);
        return new ArrayList<RefundItemAccounting>();
    }

}
