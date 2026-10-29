package com.skillsync.repository;

import com.skillsync.entity.ProjectSkill;
import com.skillsync.entity.ProjectSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectSkillRepository extends JpaRepository<ProjectSkill, ProjectSkillId> {

    @Query("SELECT ps FROM ProjectSkill ps JOIN FETCH ps.skill WHERE ps.id.projectId = :projectId")
    List<ProjectSkill> findByProjectIdWithSkill(@Param("projectId") Long projectId);

    void deleteByIdProjectId(Long projectId);
}
