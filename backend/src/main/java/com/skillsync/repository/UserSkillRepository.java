package com.skillsync.repository;

import com.skillsync.entity.UserSkill;
import com.skillsync.entity.UserSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, UserSkillId> {

    @Query("SELECT us FROM UserSkill us JOIN FETCH us.skill WHERE us.id.userId = :userId")
    List<UserSkill> findByUserIdWithSkill(@Param("userId") Long userId);

    void deleteByIdUserId(Long userId);
}
