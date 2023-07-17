package fi.hel.verkkokauppa.order.service.refund;

import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import fi.hel.verkkokauppa.order.repository.jpa.RefundItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class RefundItemService {

    private Logger log = LoggerFactory.getLogger(RefundItemService.class);

    @Autowired
    private RefundItemRepository refundItemRepository;

    public RefundItem findById(String refundItemId) {
        Optional<RefundItem> mapping = refundItemRepository.findById(refundItemId);

        if (mapping.isPresent())
            return mapping.get();

        log.debug("refundItem not found, refundItemId: " + refundItemId);
        return null;
    }

    public List<RefundItem> findByRefundId(String refundId) {
        List<RefundItem> refundItems = refundItemRepository.findByRefundId(refundId);

        if (refundItems.size() > 0)
            return refundItems;

        log.debug("refundItems not found, refundId: " + refundId);
        return new ArrayList<RefundItem>();
    }

}
