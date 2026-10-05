package com.vvmonitor.domain.entity;

import com.vvmonitor.domain.enums.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

/** Acesso de um usuario a um projeto; o criador do projeto entra como OWNER. */
@Entity
@Table(name = "project_members")
@SQLDelete(sql = "UPDATE project_members SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ProjectMember extends SoftDeletableEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberRole role;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    protected ProjectMember() {
        // exigido pelo JPA
    }

    public ProjectMember(UUID projectId, UUID userId, MemberRole role) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getUserId() {
        return userId;
    }

    public MemberRole getRole() {
        return role;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
