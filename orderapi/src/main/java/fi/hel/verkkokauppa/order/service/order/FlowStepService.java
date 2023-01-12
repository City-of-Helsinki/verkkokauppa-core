package fi.hel.verkkokauppa.order.service.order;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.FlowStepDto;
import fi.hel.verkkokauppa.order.mapper.FlowStepMapper;
import fi.hel.verkkokauppa.order.model.FlowStep;
import fi.hel.verkkokauppa.order.repository.jpa.FlowStepRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FlowStepService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FlowStepRepository flowStepRepository;

    @Autowired
    private FlowStepMapper flowStepMapper;


    public Optional<FlowStep> getFlowStepsByOrderId(String orderId) {
        return flowStepRepository.findByOrderId(orderId).stream().findFirst();
    }

    public FlowStepDto saveFlowStepsByOrderId(String orderId, FlowStepDto flowStepDto) {
        if (flowStepDto == null) {
            throw new RuntimeException("Failed to save flow steps, no data provided to save");
        }
        orderRepository.findById(orderId)
                .orElseThrow(() -> new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("order-not-found", "order with id [" + orderId + "] not found")
                ));

        // Update existing flow step or create new by orderId
        Optional<FlowStep> existingFlowStep = flowStepRepository.findByOrderId(orderId).stream().findFirst();
        if (existingFlowStep.isPresent()) {
            FlowStep flowStepToUpdate = existingFlowStep.get();
            flowStepToUpdate.setActiveStep(flowStepDto.getActiveStep());
            flowStepToUpdate.setTotalSteps(flowStepDto.getTotalSteps());

            FlowStep saved = flowStepRepository.save(flowStepToUpdate);
            if (saved == null) {
                throw new RuntimeException("Failed to update existing flow steps with orderId [" + orderId + "]");
            }
            return flowStepMapper.toDto(saved);
        } else {
            FlowStep flowStep = flowStepMapper.fromDto(flowStepDto);
            flowStep.setFlowStepId(UUIDGenerator.generateType4UUID().toString());
            flowStep.setOrderId(orderId);

            FlowStep saved = flowStepRepository.save(flowStep);
            if (saved == null) {
                throw new RuntimeException("Failed to save new flow steps with orderId [" + orderId + "]");
            }
            return flowStepMapper.toDto(saved);
        }


    }
}
