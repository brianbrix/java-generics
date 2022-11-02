package com.example.mygenerics.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;










public interface GenericRepository<T, ID> extends JpaRepository<T,ID> {
    Collection<T> selectAllFields();
    Collection<T> selectSpecificFields(List<String> fields);
}
