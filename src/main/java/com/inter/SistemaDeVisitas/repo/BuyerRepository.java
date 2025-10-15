package com.inter.SistemaDeVisitas.repo;

import com.inter.SistemaDeVisitas.entity.Buyer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuyerRepository extends JpaRepository<Buyer, Long> {
  List<Buyer> findByActiveTrueOrderByNameAsc();
  Optional<Buyer> findByNameIgnoreCase(String name);
  List<Buyer> findAllByOrderByNameAsc();
  Page<Buyer> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
