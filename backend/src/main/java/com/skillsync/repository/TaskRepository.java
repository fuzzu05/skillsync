package com.skillsync.repository;

import com.skillsync.entity.Task;
import com.skillsync.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.creator " +
           "LEFT JOIN FETCH t.assignee " +
           "WHERE t.team.id = :teamId " +
           "ORDER BY t.createdAt DESC")
    List<Task> findByTeamIdWithDetails(@Param("teamId") Long teamId);

    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.creator " +
           "LEFT JOIN FETCH t.assignee " +
           "WHERE t.team.id = :teamId AND t.status = :status " +
           "ORDER BY t.createdAt DESC")
    List<Task> findByTeamIdAndStatusWithDetails(@Param("teamId") Long teamId,
                                                @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.team tm " +
           "JOIN FETCH tm.project " +
           "WHERE t.assignee.id = :assigneeId " +
           "ORDER BY t.dueDate ASC NULLS LAST, t.createdAt DESC")
    List<Task> findByAssigneeIdWithDetails(@Param("assigneeId") Long assigneeId);
}
