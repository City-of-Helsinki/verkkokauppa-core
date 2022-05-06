package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.order.api.data.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.order.api.data.refund.RefundDto;
import fi.hel.verkkokauppa.order.api.data.refund.RefundItemDto;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RefundTransformer {
  private ModelMapper modelMapper = new ModelMapper();

  public RefundAggregateDto transformToAggregateDto(Refund refund, List<RefundItem> refundItems) {
    RefundAggregateDto dto = new RefundAggregateDto();
    dto.setRefund(modelMapper.map(refund, RefundDto.class));

    List<RefundItemDto> refundItemDtos = refundItems.stream()
            .map(refundItem -> modelMapper.map(refundItem, RefundItemDto.class))
            .collect(Collectors.toList());

    dto.setItems(refundItemDtos);

    return dto;
  }
}
