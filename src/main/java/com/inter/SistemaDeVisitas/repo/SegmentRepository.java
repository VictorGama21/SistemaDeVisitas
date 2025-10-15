package com.inter.SistemaDeVisitas.repo;

import com.inter.SistemaDeVisitas.entity.Segment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SegmentRepository extends JpaRepository<Segment, Long> {
  List<Segment> findByActiveTrueOrderByNameAsc();
  Optional<Segment> findByNameIgnoreCase(String name);
  List<Segment> findAllByOrderByNameAsc();
  Page<Segment> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
