package com.example.mygenerics.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@NoRepositoryBean
public interface GenericRepository<T, ID> extends JpaRepository<T,ID> {
    Collection<T> selectAllFields();
//    @Query(value = "SELECT #{#fields.replaceAll("r","t")} FROM #{#tableName}", nativeQuery = true)
    Collection<T> selectSpecificFields(@Param("tableName") String tableName, @Param("fields") String fields);
}
