package fi.hel.verkkokauppa.order.model.subscription;

import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionCriteria;
import fi.hel.verkkokauppa.shared.service.QueryBuilderBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionQueryBuilderBuilder implements QueryBuilderBuilder<SubscriptionCriteria> {

	@Override
	public QueryBuilder toQueryBuilder(SubscriptionCriteria criteria) {
		BoolQueryBuilder qb = QueryBuilders.boolQuery();

		if (criteria.getActiveAtDate() != null) {
			QueryBuilder rangeQuery = QueryBuilders.rangeQuery("nextDate").gte(criteria.getActiveAtDate());
			qb.should(QueryBuilders.boolQuery().should(rangeQuery));
		}

		if (!StringUtils.isEmpty(criteria.getStatus())) {
			qb.must(QueryBuilders.termQuery("status", criteria.getStatus()));
		}
		if (!StringUtils.isEmpty(criteria.getCustomerEmail())) {
			qb.must(QueryBuilders.termQuery("customerEmail", criteria.getCustomerEmail()));
		}
		if (!StringUtils.isEmpty(criteria.getNamespace())) {
			qb.must(QueryBuilders.termQuery("namespace", criteria.getNamespace()));
		}

		if (criteria.getEndDateBefore() != null) {
			QueryBuilder rangeQuery = QueryBuilders.rangeQuery("endDate").lt(criteria.getEndDateBefore());
			qb.should(QueryBuilders.boolQuery().should(rangeQuery));
		}

		return qb;
	}
}
