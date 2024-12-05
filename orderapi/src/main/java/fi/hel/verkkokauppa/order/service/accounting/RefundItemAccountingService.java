package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.order.api.data.accounting.CreateRefundAccountingRequestDto;
import fi.hel.verkkokauppa.order.api.data.accounting.ProductAccountingDto;
import fi.hel.verkkokauppa.order.api.data.accounting.RefundItemAccountingDto;
import fi.hel.verkkokauppa.order.api.data.transformer.RefundItemAccountingTransformer;
import fi.hel.verkkokauppa.order.model.accounting.RefundItemAccounting;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import fi.hel.verkkokauppa.order.repository.jpa.RefundItemAccountingRepository;
import fi.hel.verkkokauppa.order.service.refund.RefundItemService;
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

    @Autowired
    private RefundItemService refundItemService;

    public List<RefundItemAccounting> getRefundItemAccountings(String refundId) {
        List<RefundItemAccounting> accountings = refundItemAccountingRepository.findByRefundId(refundId);

        if (accountings.size() > 0) {
            return accountings;
        }

        log.debug("refundItems not found, refundId: " + refundId);
        return new ArrayList<RefundItemAccounting>();
    }

    public RefundItemAccounting createRefundItemAccounting(RefundItemAccountingDto refundItemAccountingDto) {
        RefundItemAccounting refundItemAccountingEntity = new RefundItemAccountingTransformer().transformToEntity(refundItemAccountingDto);
        this.refundItemAccountingRepository.save(refundItemAccountingEntity);
        return refundItemAccountingEntity;
    }

    public List<RefundItemAccountingDto> createRefundItemAccountings(CreateRefundAccountingRequestDto request) {
        List<RefundItemAccountingDto> refundItemAccountings = new ArrayList<>();

        String refundId = request.getRefundId();
        List<ProductAccountingDto> productAccountingDtos = request.getDtos();

        List<RefundItem> refundItems = refundItemService.findByRefundId(refundId);
        for (RefundItem refundItem : refundItems) {
            String refundItemProductId = refundItem.getProductId();

            // if item has price then create accounting for it
            if(StringUtils.getDoubleFromString(refundItem.getRowPriceTotal()) != 0.0 ) {
                for (ProductAccountingDto productAccountingDto : productAccountingDtos) {
                    String productId = productAccountingDto.getProductId();

                    if (productId.equalsIgnoreCase(refundItemProductId)) {
                        RefundItemAccountingDto refundItemAccountingDto = new RefundItemAccountingDto(refundItem, productAccountingDto);
                        // Add extra data by setter
                        refundItemAccountingDto.setRefundCreatedAt(productAccountingDto.getRefundCreatedAt());
                        refundItemAccountingDto.setMerchantId(productAccountingDto.getMerchantId());
                        refundItemAccountingDto.setRefundTransactionId(productAccountingDto.getRefundTransactionId());
                        refundItemAccountingDto.setNamespace(productAccountingDto.getNamespace());
                        createRefundItemAccounting(refundItemAccountingDto);
                        refundItemAccountings.add(refundItemAccountingDto);
                    }
                }
            }
        }
        return refundItemAccountings;
    }

}
