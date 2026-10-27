package com.vvmonitor.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Teste de um projeto (RF9), que cobre um ou mais requisitos (test_coverage). Codigo T1, T2... gerado como o
 * dos elementos. A coluna status (NOT_RUN/PASSED/FAILED) existe no banco, mas fica fora por enquanto
 * (decisao de 21/10): todo teste nasce e permanece NOT_RUN, o valor padrao da coluna.
 */
@Entity
@Table(name = "tests")
@SQLDelete(sql = "UPDATE tests SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class TestCase extends BaseEntity {

    public static final String CODE_PREFIX = "T";

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(nullable = false, length = 20, updatable = false)
    private String code;

    @Column(nullable = false)
    private String description;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    protected TestCase() {
        // exigido pelo JPA
    }

    public TestCase(UUID projectId, int number, String description, UUID createdBy) {
        this.projectId = projectId;
        this.code = CODE_PREFIX + number;
        this.description = description.strip();
        this.createdBy = createdBy;
    }

    public void update(String description) {
        this.description = description.strip();
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    /** Numero do codigo (T12 -> 12), para ordenar T2 antes de T10. */
    public int codeNumber() {
        return Integer.parseInt(code.substring(CODE_PREFIX.length()));
    }
}
