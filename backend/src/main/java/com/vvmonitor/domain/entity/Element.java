package com.vvmonitor.domain.entity;

import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/** Requisito funcional, nao funcional ou regra de negocio de um projeto (RF5, RF6). */
@Entity
@Table(name = "elements")
@SQLDelete(sql = "UPDATE elements SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Element extends BaseEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ElementKind kind;

    @Column(nullable = false, length = 20, updatable = false)
    private String code;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Priority priority;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    protected Element() {
        // exigido pelo JPA
    }

    /** Novo elemento entra no modelo atual; fica pendente ate a proxima versao (UC-07, passo 5). */
    public Element(UUID projectId, ElementKind kind, String code, String description, Priority priority,
                   UUID createdBy) {
        this.projectId = projectId;
        this.kind = kind;
        this.code = code;
        this.description = description.strip();
        this.priority = priority;
        this.createdBy = createdBy;
    }

    /**
     * RF14: edita descricao e prioridade. Tipo e codigo nao mudam (o codigo identifica o elemento).
     * A edicao vale na hora; ate a proxima versao, o elemento aparece como alteracao pendente.
     */
    public void update(String description, Priority priority) {
        this.description = description.strip();
        this.priority = priority;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public ElementKind getKind() {
        return kind;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    /** Numero do codigo (RF12 -> 12), para ordenar RF2 antes de RF10. */
    public int codeNumber() {
        return Integer.parseInt(code.substring(kind.prefix().length()));
    }
}
