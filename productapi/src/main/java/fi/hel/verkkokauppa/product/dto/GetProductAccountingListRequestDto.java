package fi.hel.verkkokauppa.product.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class GetProductAccountingListRequestDto {

    private List<String> productIds;

}
