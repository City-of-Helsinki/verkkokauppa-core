package fi.hel.verkkokauppa.order.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import fi.hel.verkkokauppa.order.api.data.FlowStepDto;
import fi.hel.verkkokauppa.order.model.FlowStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FlowStepMapper extends AbstractModelMapper<FlowStep, FlowStepDto> {

    @Autowired
    public FlowStepMapper(ObjectMapper mapper) {
        super(mapper, FlowStep::new, FlowStepDto::new);
    }
}
