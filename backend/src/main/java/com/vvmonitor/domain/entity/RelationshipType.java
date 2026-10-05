package com.vvmonitor.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

/**
 * Tipo de relacionamento (RF8). Sem projeto: pre-definido e global (Dependencia, Conflito, Refinamento,
 * Similaridade, criados na migracao V1). Com projeto: personalizado, criado pelo ator (UC-06, alternativa).
 */
@Entity
@Table(name = "relationship_types")
@SQLDelete(sql = "UPDATE relationship_types SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class RelationshipType extends SoftDeletableEntity {

    /** Nome do tipo pre-definido usado no alerta de exclusao ("perdem uma dependencia"). */
    public static final String DEPENDENCY = "Dependência";

    @Column(name = "project_id", updatable = false)
    private UUID projectId;

    @Column(nullable = false, length = 60)
    private String name;

    /** Simetrico: A-B e B-A sao a mesma relacao (ex.: Conflito). */
    @Column(name = "is_symmetric", nullable = false, updatable = false)
    private boolean symmetric;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RelationshipType() {
        // exigido pelo JPA
    }

    /** Tipo personalizado do projeto; o UC-06 pede so o nome, e ele e tratado como dirigido. */
    public static RelationshipType custom(UUID projectId, String name) {
        RelationshipType type = new RelationshipType();
        type.projectId = projectId;
        type.name = name.strip();
        type.symmetric = false;
        return type;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public boolean isSymmetric() {
        return symmetric;
    }

    public boolean isCustom() {
        return projectId != null;
    }

    public boolean isDependency() {
        return !isCustom() && DEPENDENCY.equals(name);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Pre-definidos valem para todos os projetos; personalizados, so para o proprio. */
    public boolean isAvailableIn(UUID projectId) {
        return this.projectId == null || this.projectId.equals(projectId);
    }
}
