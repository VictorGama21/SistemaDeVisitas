package com.inter.SistemaDeVisitas.repo;

import com.inter.SistemaDeVisitas.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByActiveTrueOrderByNameAsc();
    List<Store> findByActiveFalseOrderByNameAsc();
    List<Store> findAllByOrderByNameAsc();
    long countByActiveTrue();
}
