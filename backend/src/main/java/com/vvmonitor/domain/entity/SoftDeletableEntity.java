package com.vvmonitor.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.time.Instant;
import java.util.UUID;

/**
 * Base minima: id UUID e soft delete. Usada direto pelas tabelas sem created_at/updated_at
 * (ex.: project_members, que tem added_at); as demais estendem {@link BaseEntity}.
 * Cada entidade concreta declara @SQLDelete e @SQLRestriction com o nome da sua tabela,
 * para que delete() marque deleted_at e as consultas ignorem registros removidos.
 */
@MappedSuperclass
public abstract class SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
