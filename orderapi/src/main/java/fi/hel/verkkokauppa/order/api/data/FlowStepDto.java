package fi.hel.verkkokauppa.order.api.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlowStepDto {
    private String flowStepId;
    private String orderId;
    private Integer activeStep;
    private Integer totalSteps;
}
