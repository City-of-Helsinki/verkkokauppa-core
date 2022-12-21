package fi.hel.verkkokauppa.order.api.data;

import lombok.Data;

@Data
public class FlowStepDto {
    private String flowStepId;
    private String orderId;
    private Integer activeStep;
    private Integer totalSteps;
}
