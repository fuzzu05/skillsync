package com.skillsync.repository;

import com.skillsync.entity.ProjectApplication;
import com.skillsync.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {

    @Query("SELECT pa FROM ProjectApplication pa " +
           "JOIN FETCH pa.applicant " +
           "JOIN FETCH pa.project " +
           "WHERE pa.project.id = :projectId " +
           "ORDER BY pa.createdAt DESC")
    List<ProjectApplication> findByProjectIdWithDetails(@Param("projectId") Long projectId);

    @Query("SELECT pa FROM ProjectApplication pa " +
           "JOIN FETCH pa.applicant " +
           "JOIN FETCH pa.project " +
           "WHERE pa.project.id = :projectId AND pa.status = :status " +
           "ORDER BY pa.createdAt DESC")
    List<ProjectApplication> findByProjectIdAndStatusWithDetails(@Param("projectId") Long projectId,
                                                                 @Param("status") ApplicationStatus status);

    @Query("SELECT pa FROM ProjectApplication pa " +
           "JOIN FETCH pa.project p " +
           "JOIN FETCH p.owner " +
           "WHERE pa.applicant.id = :applicantId " +
           "ORDER BY pa.createdAt DESC")
    List<ProjectApplication> findByApplicantIdWithProject(@Param("applicantId") Long applicantId);

    Optional<ProjectApplication> findByProjectIdAndApplicantId(Long projectId, Long applicantId);

    boolean existsByProjectIdAndApplicantId(Long projectId, Long applicantId);
}
