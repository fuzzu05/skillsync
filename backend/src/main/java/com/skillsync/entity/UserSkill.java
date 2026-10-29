package com.skillsync.entity;

import com.skillsync.enums.Proficiency;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "user_skills")
public class UserSkill {

    @EmbeddedId
    private UserSkillId id = new UserSkillId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Proficiency proficiency = Proficiency.BEGINNER;

    @Column(name = "years_of_experience", precision = 3, scale = 1)
    private BigDecimal yearsOfExperience = BigDecimal.ZERO;

    public UserSkill() {
    }

    public UserSkill(User user, Skill skill, Proficiency proficiency, BigDecimal yearsOfExperience) {
        this.user = user;
        this.skill = skill;
        this.id = new UserSkillId(user.getId(), skill.getId());
        this.proficiency = proficiency;
        this.yearsOfExperience = yearsOfExperience != null ? yearsOfExperience : BigDecimal.ZERO;
    }

    // Getters and Setters
    public UserSkillId getId() {
        return id;
    }

    public void setId(UserSkillId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null && user.getId() != null) {
            this.id.setUserId(user.getId());
        }
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
        if (skill != null && skill.getId() != null) {
            this.id.setSkillId(skill.getId());
        }
    }

    public Proficiency getProficiency() {
        return proficiency;
    }

    public void setProficiency(Proficiency proficiency) {
        this.proficiency = proficiency;
    }

    public BigDecimal getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(BigDecimal yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }
}
