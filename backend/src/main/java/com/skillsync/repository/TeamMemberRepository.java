package com.skillsync.repository;

import com.skillsync.entity.TeamMember;
import com.skillsync.entity.TeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {

    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.user WHERE tm.id.teamId = :teamId")
    List<TeamMember> findByTeamIdWithUser(@Param("teamId") Long teamId);

    long countByIdTeamId(Long teamId);

    boolean existsByIdTeamIdAndIdUserId(Long teamId, Long userId);

    Optional<TeamMember> findByIdTeamIdAndIdUserId(Long teamId, Long userId);

    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.team t JOIN FETCH t.project WHERE tm.id.userId = :userId")
    List<TeamMember> findByUserIdWithProject(@Param("userId") Long userId);
}
