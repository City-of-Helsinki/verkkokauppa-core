package fi.hel.verkkokauppa.order.model.recurringorder;

import fi.hel.verkkokauppa.order.api.data.recurringorder.RecurringOrderCriteria;
import fi.hel.verkkokauppa.shared.service.QueryBuilderBuilder;
import fi.hel.verkkokauppa.utils.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

@Component
public class RecurringOrderQueryBuilderBuilder implements QueryBuilderBuilder<RecurringOrderCriteria> {

	@Override
	public QueryBuilder toQueryBuilder(RecurringOrderCriteria criteria) {
		BoolQueryBuilder qb = QueryBuilders.boolQuery();

		if (criteria.getActiveAtDate() != null) {
			QueryBuilder rangeQuery = QueryBuilders.rangeQuery("field").gte(criteria.getActiveAtDate());
			qb.should(QueryBuilders.boolQuery().should(rangeQuery));
		}

		if (!StringUtils.isEmpty(criteria.getStatus())
				&& Period.getAllowedPeriods().contains(criteria.getStatus())) {
			qb.must(QueryBuilders.termQuery("status", criteria.getStatus()));
		}
		if (!StringUtils.isEmpty(criteria.getCustomerId())) {
			qb.must(QueryBuilders.termQuery("customerId", criteria.getCustomerId()));
		}
		if (!StringUtils.isEmpty(criteria.getMerchantNamespace())) {
			qb.must(QueryBuilders.termQuery("merchantNamespace", criteria.getMerchantNamespace()));
		}

		return qb;
	}
}
