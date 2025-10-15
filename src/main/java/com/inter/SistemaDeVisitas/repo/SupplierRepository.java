package com.inter.SistemaDeVisitas.repo;

import com.inter.SistemaDeVisitas.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
  List<Supplier> findByActiveTrueOrderByNameAsc();
  Optional<Supplier> findByNameIgnoreCase(String name);
  List<Supplier> findAllByOrderByNameAsc();
  Page<Supplier> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
