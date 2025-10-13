package com.inter.SistemaDeVisitas.repo;

import com.inter.SistemaDeVisitas.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByActiveTrueOrderByNameAsc();
    List<Store> findByActiveFalseOrderByNameAsc();
    List<Store> findAllByOrderByNameAsc();
    long countByActiveTrue();
    Optional<Store> findByNameIgnoreCase(String name);
    List<Store> findByNameIn(Collection<String> names);
}
