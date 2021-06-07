package fi.hel.verkkokauppa.shared.service;

import org.elasticsearch.index.query.QueryBuilder;

@FunctionalInterface
public interface QueryBuilderBuilder<C> {

	QueryBuilder toQueryBuilder(C criteria);
}
