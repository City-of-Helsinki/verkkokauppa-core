package fi.hel.verkkokauppa.order.api.data.transformer;

import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RefundTransformer {
  private ModelMapper modelMapper = new ModelMapper();

  public RefundDto transformToDto(Refund refund) {
    return modelMapper.map(refund, RefundDto.class);
  }

  public RefundItemDto transformToDto(RefundItem refundItem) {
    return modelMapper.map(refundItem, RefundItemDto.class);
  }

  public RefundAggregateDto transformToDto(Refund refund, List<RefundItem> refundItems) {
    RefundAggregateDto dto = new RefundAggregateDto();
    dto.setRefund(transformToDto(refund));

    List<RefundItemDto> refundItemDtos = refundItems.stream()
            .map(this::transformToDto)
            .collect(Collectors.toList());

    dto.setItems(refundItemDtos);

    return dto;
  }
}
