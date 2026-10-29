package com.skillsync.repository;

import com.skillsync.entity.Project;
import com.skillsync.enums.ProjectStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerId(Long ownerId);

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdWithPessimisticLock(@Param("id") Long id);

    @Query("SELECT p FROM Project p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.summary) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Project> searchProjects(@Param("query") String query,
                                @Param("status") ProjectStatus status,
                                Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p " +
           "JOIN p.projectSkills ps " +
           "WHERE (:status IS NULL OR p.status = :status) " +
           "AND ps.skill.id = :skillId")
    Page<Project> findBySkillIdAndStatus(@Param("skillId") Long skillId,
                                         @Param("status") ProjectStatus status,
                                         Pageable pageable);
}
