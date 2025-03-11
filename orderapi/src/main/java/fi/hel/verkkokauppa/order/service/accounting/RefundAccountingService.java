package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.accounting.RefundAccountingDto;
import fi.hel.verkkokauppa.order.api.data.accounting.RefundItemAccountingDto;
import fi.hel.verkkokauppa.order.api.data.transformer.RefundAccountingTransformer;
import fi.hel.verkkokauppa.order.model.accounting.RefundAccounting;
import fi.hel.verkkokauppa.order.repository.jpa.RefundAccountingRepository;
import fi.hel.verkkokauppa.order.service.refund.RefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class RefundAccountingService {

    @Autowired
    RefundService refundService;

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

    public RefundAccounting getRefundAccounting(String refundId) {
        Optional<RefundAccounting> mapping = refundAccountingRepository.findById(refundId);

        if (mapping.isPresent()) {
            return mapping.get();
        }

        log.warn("refund accounting not found, refundId: " + refundId);
        return null;
    }

    public RefundAccountingDto createRefundAccounting(String refundId, String orderId, String namespace, List<RefundItemAccountingDto> refundItemAccountings) {
        LocalDateTime createdAt = DateTimeUtil.getFormattedDateTime();
        RefundAccountingDto refundAccountingDto = new RefundAccountingDto(refundId, orderId, namespace, createdAt, refundItemAccountings);
        createRefundAccounting(refundAccountingDto);

        return refundAccountingDto;
    }

    public RefundAccounting createRefundAccounting(RefundAccountingDto refundAccountingDto) {
        RefundAccounting productAccountingEntity = new RefundAccountingTransformer().transformToEntity(refundAccountingDto);
        this.refundAccountingRepository.save(productAccountingEntity);

        refundService.accountingCreated(refundAccountingDto.getRefundId());

        return productAccountingEntity;
    }

}
