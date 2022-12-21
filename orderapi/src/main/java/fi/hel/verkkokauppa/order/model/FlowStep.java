package fi.hel.verkkokauppa.order.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "flowsteps")
@Setter
@Getter
@NoArgsConstructor
public class FlowStep {

    @Id
    private String flowStepId;

    @Field(type = FieldType.Text)
    private String orderId;

    @Field(type = FieldType.Integer)
    private Integer activeStep;

    @Field(type = FieldType.Integer)
    private Integer totalSteps;
}
