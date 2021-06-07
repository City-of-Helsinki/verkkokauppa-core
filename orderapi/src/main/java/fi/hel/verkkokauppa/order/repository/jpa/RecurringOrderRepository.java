package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.shared.repository.jpa.BaseRepository;
import fi.hel.verkkokauppa.order.model.recurringorder.RecurringOrder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecurringOrderRepository extends BaseRepository<RecurringOrder, String> {

    List<RecurringOrder> findByCustomerId(String customerId);
}