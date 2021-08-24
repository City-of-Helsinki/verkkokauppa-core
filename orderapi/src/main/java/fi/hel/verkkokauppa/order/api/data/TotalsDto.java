package fi.hel.verkkokauppa.order.api.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import javax.validation.constraints.PositiveOrZero;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TotalsDto {

    @PositiveOrZero(message = "priceNet must be numeric and positive")
    private BigDecimal priceNet;

    @PositiveOrZero(message = "priceVat must be numeric and positive")
    private BigDecimal priceVat;

    @PositiveOrZero(message = "priceTotal must be numeric and positive")
    private BigDecimal priceTotal;


}