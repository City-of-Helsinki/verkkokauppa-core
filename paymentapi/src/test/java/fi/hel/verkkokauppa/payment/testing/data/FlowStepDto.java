package fi.hel.verkkokauppa.payment.testing.data;

import lombok.Data;

@Data
public class FlowStepDto {
    private String flowStepId;
    private String orderId;
    private Integer activeStep;
    private Integer totalSteps;
}
