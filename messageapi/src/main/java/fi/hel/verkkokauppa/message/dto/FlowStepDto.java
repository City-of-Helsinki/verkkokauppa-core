package fi.hel.verkkokauppa.message.dto;

import lombok.Data;

@Data
public class FlowStepDto {
    private String flowStepId;
    private String orderId;
    private Integer activeStep;
    private Integer totalSteps;
}
