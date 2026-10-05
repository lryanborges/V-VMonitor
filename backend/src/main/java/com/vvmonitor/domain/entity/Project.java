package com.vvmonitor.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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

    protected Project() {
        // exigido pelo JPA
    }

    public Project(String name, String description, UUID ownerId) {
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.ownerId = ownerId;
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
