package com.vvmonitor.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
@SQLDelete(sql = "UPDATE projects SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Project extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column
    private String description;

    // referencia por id: o dono e carregado so quando a resposta precisa do nome
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    /** RF10: quando e por quem o modelo foi submetido pela ultima vez (nulos ate a primeira submissao). */
    @Column(name = "last_submitted_at")
    private Instant lastSubmittedAt;

    @Column(name = "last_submitted_by")
    private UUID lastSubmittedBy;

    protected Project() {
        // exigido pelo JPA
    }

    public Project(String name, String description, UUID ownerId) {
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.ownerId = ownerId;
    }

    public void registerSubmission(UUID userId, Instant at) {
        this.lastSubmittedBy = userId;
        this.lastSubmittedAt = at;
    }

    public Instant getLastSubmittedAt() {
        return lastSubmittedAt;
    }

    public UUID getLastSubmittedBy() {
        return lastSubmittedBy;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }
}
