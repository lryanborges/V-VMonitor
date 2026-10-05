package com.vvmonitor.domain.entity;

import com.vvmonitor.domain.enums.SubmissionStatus;
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

/** Relacao entre dois elementos do mesmo projeto (RF7): "source [tipo] target", ex.: RF7 depende de RF5. */
@Entity
@Table(name = "relationships")
@SQLDelete(sql = "UPDATE relationships SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Relationship extends SoftDeletableEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "type_id", nullable = false)
    private UUID typeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false, length = 10)
    private SubmissionStatus submissionStatus;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Relationship() {
        // exigido pelo JPA
    }

    /** Nova relacao entra no modelo atual como rascunho, como os elementos. */
    public Relationship(UUID projectId, UUID sourceId, UUID targetId, UUID typeId, UUID createdBy) {
        this.projectId = projectId;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.typeId = typeId;
        this.submissionStatus = SubmissionStatus.DRAFT;
        this.createdBy = createdBy;
    }

    /**
     * RF14: troca o tipo e/ou a ordem das pontas (as pontas sao as mesmas; ligar a outro elemento e outra relacao).
     * Como no elemento, a edicao volta a ser uma alteracao pendente.
     */
    public void update(UUID typeId, UUID sourceId, UUID targetId) {
        this.typeId = typeId;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.submissionStatus = SubmissionStatus.DRAFT;
    }

    public boolean touches(UUID elementId) {
        return sourceId.equals(elementId) || targetId.equals(elementId);
    }

    /** A outra ponta da relacao, vista a partir de um dos elementos. */
    public UUID otherEnd(UUID elementId) {
        return sourceId.equals(elementId) ? targetId : sourceId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public UUID getTypeId() {
        return typeId;
    }

    public SubmissionStatus getSubmissionStatus() {
        return submissionStatus;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
