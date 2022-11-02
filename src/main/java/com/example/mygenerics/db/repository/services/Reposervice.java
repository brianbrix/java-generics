package com.example.mygenerics.db.repository.services;

import java.util.Collection;
import java.util.List;

public interface Reposervice<T> {
    Collection<T> selectAllFields();
    Collection<T> selectSpecificFields(List<String> fields);
}
