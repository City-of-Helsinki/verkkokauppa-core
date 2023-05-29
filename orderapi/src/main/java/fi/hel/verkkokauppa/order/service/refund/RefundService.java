package fi.hel.verkkokauppa.order.service.refund;

import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.model.refund.RefundAccountingStatus;
import fi.hel.verkkokauppa.order.repository.jpa.RefundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;


@Component
public class RefundService {

    private Logger log = LoggerFactory.getLogger(RefundService.class);

    @Autowired
    private RefundRepository refundRepository;

    public void markAsAccounted(String refundId) {
        Refund refund = findById(refundId);
        refund.setAccounted(LocalDate.now());
        refund.setAccountingStatus(RefundAccountingStatus.EXPORTED);
        refundRepository.save(refund);
        log.debug("marked refund accounted, refundId: " + refund.getOrderId());
    }

    public Refund findById(String refundId) {
        Optional<Refund> refund = refundRepository.findById(refundId);
        return refund.get();
    }

}
