package ch.rail.r2p.persistence.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Component;

@Component
class CustomInsertRepositoryImpl<T> implements CustomInsertRepository<T> {

    @Autowired
    private JdbcAggregateTemplate jdbcAggregateTemplate;

    @Override
    public void insert(T entity) {
        jdbcAggregateTemplate.insert(entity);
    }

}
