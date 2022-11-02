package com.example.mygenerics.db.repository.services;

import com.example.mygenerics.db.repository.GenericRepository;

import java.util.Collection;
import java.util.List;


public class ReposerviceImpl<T> implements Reposervice<T> {

    @Override
    public Collection<T> selectAllFields() {
        return null;
    }

    @Override
    public Collection<T> selectSpecificFields(List fields) {
        return null;
    }
}
