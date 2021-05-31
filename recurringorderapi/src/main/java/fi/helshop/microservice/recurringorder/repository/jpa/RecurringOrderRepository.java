package fi.helshop.microservice.recurringorder.repository.jpa;

import fi.helshop.microservice.shared.repository.jpa.BaseRepository;
import fi.helshop.microservice.recurringorder.model.recurringorder.RecurringOrder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecurringOrderRepository extends BaseRepository<RecurringOrder, String> {

    List<RecurringOrder> findByCustomerId(String customerId);
}