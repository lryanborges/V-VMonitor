package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.entity.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelationshipRepository extends JpaRepository<Relationship, UUID> {

    List<Relationship> findAllByProjectIdOrderByCreatedAt(UUID projectId);

    /** Busca restrita ao projeto: relacao de outro projeto e tratada como inexistente. */
    Optional<Relationship> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsBySourceIdAndTargetIdAndTypeId(UUID sourceId, UUID targetId, UUID typeId);

    /** Duplicata ao editar: outra relacao (que nao a propria) com as mesmas pontas e tipo. */
    boolean existsBySourceIdAndTargetIdAndTypeIdAndIdNot(UUID sourceId, UUID targetId, UUID typeId, UUID id);

    /** RF15: remove (soft delete) todas as relacoes de um elemento que esta sendo excluido. */
    @Modifying
    @Query(value = """
            UPDATE relationships SET deleted_at = now()
             WHERE deleted_at IS NULL AND (source_id = :elementId OR target_id = :elementId)
            """, nativeQuery = true)
    int softDeleteAllOfElement(@Param("elementId") UUID elementId);
}
