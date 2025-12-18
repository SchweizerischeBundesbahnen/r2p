package ch.rail.r2p.persistence.impl;

public interface CustomInsertRepository<T> {

    void insert(T entity);

}
