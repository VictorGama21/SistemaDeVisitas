package com.inter.SistemaDeVisitas.repo;

import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.Visit;
import com.inter.SistemaDeVisitas.entity.VisitStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;


import java.time.LocalDate;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
  @EntityGraph(attributePaths = {"stores", "buyer", "supplier", "segment"})
  @Query("select distinct v from Visit v join v.stores s where s = :store order by v.scheduledDate desc")
  List<Visit> findByStoreOrderByScheduledDateDesc(@Param("store") Store store);
  @EntityGraph(attributePaths = {"stores", "buyer", "supplier", "segment"})
  Optional<Visit> findDetailedById(Long id);

  long countByStatus(VisitStatus status);

  long countByScheduledDateBetween(LocalDate start, LocalDate end);

  @Query("select count(distinct v) from Visit v join v.stores s where s = :store and v.scheduledDate between :start and :end")
  long countByStoreAndScheduledDateBetween(@Param("store") Store store,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);
  
  @EntityGraph(attributePaths = {"stores", "buyer", "supplier", "segment"})
  List<Visit> findTop10ByOrderByScheduledDateDesc();

  @EntityGraph(attributePaths = {"stores", "buyer", "supplier", "segment"})
  @Query("select distinct v from Visit v join v.stores s where s = :store order by v.scheduledDate asc")
  List<Visit> findByStoreOrderByScheduledDateAsc(@Param("store") Store store);

  @Query("select v from Visit v join fetch v.stores where v.id in :ids")
  List<Visit> findAllWithStoresByIdIn(@Param("ids") List<Long> ids);

  @EntityGraph(attributePaths = {"stores", "buyer", "supplier", "segment"})
  @Query("select distinct v from Visit v join v.stores s where s = :store and v.scheduledDate between :start and :end order by v.scheduledDate asc")
  List<Visit> findByStoreAndScheduledDateBetween(@Param("store") Store store,
                                                  @Param("start") LocalDate start,
                                                  @Param("end") LocalDate end);
  
  @EntityGraph(attributePaths = {"stores", "buyer", "supplier", "segment"})
  List<Visit> findTop20ByScheduledDateGreaterThanEqualOrderByScheduledDateAsc(LocalDate start);

  @EntityGraph(attributePaths = {"stores", "buyer", "supplier", "segment"})
  @Query("select distinct v from Visit v join v.stores s where s = :store and v.scheduledDate = :date order by v.createdAt asc")
  List<Visit> findByStoreAndScheduledDate(@Param("store") Store store,
                                          @Param("date") LocalDate date);

  @Query("select count(distinct v) from Visit v join v.stores s where s = :store and v.status = :status and v.scheduledDate < :before")
  long countByStoreAndStatusBefore(@Param("store") Store store,
                                   @Param("status") VisitStatus status,
                                   @Param("before") LocalDate before);

  @EntityGraph(attributePaths = {"stores", "buyer", "supplier", "segment"})
  @Query("""
      select distinct v from Visit v
      left join v.stores s
      where (:store is null or s = :store)
        and v.scheduledDate >= coalesce(:start, v.scheduledDate)
        and v.scheduledDate <= coalesce(:end, v.scheduledDate)
      order by v.scheduledDate desc
      """)
  List<Visit> findByStoreAndDateRange(@Param("store") Store store,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);

  @Query("""
      select v.status as status, count(distinct v) as total
      from Visit v join v.stores s
      where (:store is null or s = :store)
        and v.scheduledDate >= coalesce(:start, v.scheduledDate)
        and v.scheduledDate <= coalesce(:end, v.scheduledDate)
      group by v.status
      order by v.status
      """)
  List<Object[]> countByStoreAndStatusBetween(@Param("store") Store store,
                                              @Param("start") LocalDate start,
                                              @Param("end") LocalDate end);

  @Query("""
      select v.scheduledDate as scheduledDate, count(distinct v) as total
      from Visit v join v.stores s
      where (:store is null or s = :store)
        and v.scheduledDate >= coalesce(:start, v.scheduledDate)
        and v.scheduledDate <= coalesce(:end, v.scheduledDate)
      group by v.scheduledDate
      order by v.scheduledDate asc
      """)
  List<Object[]> countDailyByStoreBetween(@Param("store") Store store,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);
}
