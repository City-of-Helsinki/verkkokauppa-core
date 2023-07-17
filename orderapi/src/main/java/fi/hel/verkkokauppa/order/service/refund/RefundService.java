package fi.hel.verkkokauppa.order.service.refund;

import fi.hel.verkkokauppa.order.constants.RefundAccountingStatusEnum;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.repository.jpa.RefundRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;


@Component
@Slf4j
public class RefundService {

    @Autowired
    private RefundRepository refundRepository;

    public void markAsAccounted(String refundId) {
        Refund refund = findById(refundId);
        refund.setAccounted(LocalDate.now());
        refund.setAccountingStatus(RefundAccountingStatusEnum.EXPORTED);
        refundRepository.save(refund);
        log.debug("marked refund accounted, refundId: " + refund.getRefundId());
    }

    public void accountingCreated(String refundId) {
        Refund refund = findById(refundId);
        refund.setAccountingStatus(RefundAccountingStatusEnum.CREATED);
        refundRepository.save(refund);
        log.debug("Refund accounting created, refundId: " + refund.getRefundId());
    }

    public Refund findById(String refundId) {
        Optional<Refund> refund = refundRepository.findById(refundId);
        return refund.get();
    }

}
