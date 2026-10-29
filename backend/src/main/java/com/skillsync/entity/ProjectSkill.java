package com.skillsync.entity;

import com.skillsync.enums.Proficiency;
import jakarta.persistence.*;

@Entity
@Table(name = "project_skills")
public class ProjectSkill {

    @EmbeddedId
    private ProjectSkillId id = new ProjectSkillId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId")
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_proficiency", nullable = false, length = 20)
    private Proficiency minProficiency = Proficiency.BEGINNER;

    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory = true;

    public ProjectSkill() {
    }

    public ProjectSkill(Project project, Skill skill, Proficiency minProficiency, boolean mandatory) {
        this.project = project;
        this.skill = skill;
        this.id = new ProjectSkillId(project.getId(), skill.getId());
        this.minProficiency = minProficiency != null ? minProficiency : Proficiency.BEGINNER;
        this.mandatory = mandatory;
    }

    // Getters and Setters
    public ProjectSkillId getId() {
        return id;
    }

    public void setId(ProjectSkillId id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
        if (project != null && project.getId() != null) {
            this.id.setProjectId(project.getId());
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

    public Proficiency getMinProficiency() {
        return minProficiency;
    }

    public void setMinProficiency(Proficiency minProficiency) {
        this.minProficiency = minProficiency;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
}
