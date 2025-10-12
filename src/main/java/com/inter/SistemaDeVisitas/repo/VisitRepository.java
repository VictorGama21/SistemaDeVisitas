package com.inter.SistemaDeVisitas.repo;

import com.inter.SistemaDeVisitas.entity.Visit;
import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
  List<Visit> findByStoreOrderByScheduledAtDesc(Store store);
  long countByStatus(VisitStatus status);
}
