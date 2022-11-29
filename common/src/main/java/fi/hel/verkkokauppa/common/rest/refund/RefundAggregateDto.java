package fi.hel.verkkokauppa.common.rest.refund;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RefundAggregateDto {

    @NotNull(message = "refund required")
    private RefundDto refund;
    @NotNull(message = "items required")
    private List<RefundItemDto> items = new ArrayList<>();

}
