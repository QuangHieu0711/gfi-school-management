package com.gfi.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gfi.backend.models.entities.WeekConfig;

@Repository
public interface WeekConfigRepository extends JpaRepository<WeekConfig, Long> {

    @Query("""
            select w
            from WeekConfig w
            where w.deletedFlag = 0
              and w.schoolYear.id = :schoolYearId
              and (:semesterId is null or w.semester.id = :semesterId)
            order by w.semester.semesterOrder asc, w.weekNumber asc, w.id asc
            """)
    List<WeekConfig> search(Long schoolYearId, Long semesterId);

    List<WeekConfig> findBySemesterIdAndDeletedFlagOrderByWeekNumberAscIdAsc(Long semesterId, Integer deletedFlag);

    List<WeekConfig> findBySemesterIdAndDeletedFlagOrderByWeekNumberDescIdAsc(Long semesterId, Integer deletedFlag);

    List<WeekConfig> findBySemesterIdAndDeletedFlagOrderByIdAsc(Long semesterId, Integer deletedFlag);

    boolean existsBySemesterIdAndDeletedFlag(Long semesterId, Integer deletedFlag);

    Optional<WeekConfig> findBySemesterIdAndWeekNumberAndDeletedFlag(Long semesterId, Integer weekNumber, Integer deletedFlag);

    Optional<WeekConfig> findByIdAndDeletedFlag(Long id, Integer deletedFlag);
}
