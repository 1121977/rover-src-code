package ru.ctf.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import java.util.List;

public abstract class DaoImpl<T> implements Dao<T> {

    protected final Class<T> entityClass;

    @Autowired
    protected LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean;

    public DaoImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public long save(T t) {
        return 0L;
    }

    public List<T> findAll() {
        return null;
    }

}
