package ru.ctf.dao;

import java.util.List;

public interface Dao<T> {
    long save(T t);
    List<T> findAll();
}
