package com.inter.SistemaDeVisitas.repo;

import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.Visit;
import com.inter.SistemaDeVisitas.entity.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
  @Query("select distinct v from Visit v join v.stores s where s = :store order by v.scheduledDate desc")
  List<Visit> findByStoreOrderByScheduledDateDesc(@Param("store") Store store);

  long countByStatus(VisitStatus status);

  long countByScheduledDateBetween(LocalDate start, LocalDate end);

  @Query("select count(distinct v) from Visit v join v.stores s where s = :store and v.scheduledDate between :start and :end")
  long countByStoreAndScheduledDateBetween(@Param("store") Store store,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);

  List<Visit> findTop10ByOrderByScheduledDateDesc();

  @Query("select distinct v from Visit v join v.stores s where s = :store order by v.scheduledDate asc")
  List<Visit> findByStoreOrderByScheduledDateAsc(@Param("store") Store store);

  @Query("select v from Visit v join fetch v.stores where v.id in :ids")
  List<Visit> findAllWithStoresByIdIn(@Param("ids") List<Long> ids);

  @Query("select distinct v from Visit v join v.stores s where s = :store and v.scheduledDate between :start and :end order by v.scheduledDate asc")
  List<Visit> findByStoreAndScheduledDateBetween(@Param("store") Store store,
                                                  @Param("start") LocalDate start,
                                                  @Param("end") LocalDate end);

  List<Visit> findTop20ByScheduledDateGreaterThanEqualOrderByScheduledDateAsc(LocalDate start);
}
