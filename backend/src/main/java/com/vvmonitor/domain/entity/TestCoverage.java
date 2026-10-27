package com.vvmonitor.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

/** Vinculo entre um teste e um requisito que ele cobre (RF9, RF13). Regras de negocio nao sao cobertas. */
@Entity
@Table(name = "test_coverage")
@SQLDelete(sql = "UPDATE test_coverage SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class TestCoverage extends SoftDeletableEntity {

    @Column(name = "test_id", nullable = false, updatable = false)
    private UUID testId;

    @Column(name = "element_id", nullable = false, updatable = false)
    private UUID elementId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TestCoverage() {
        // exigido pelo JPA
    }

    public TestCoverage(UUID testId, UUID elementId) {
        this.testId = testId;
        this.elementId = elementId;
    }

    public UUID getTestId() {
        return testId;
    }

    public UUID getElementId() {
        return elementId;
    }
}
