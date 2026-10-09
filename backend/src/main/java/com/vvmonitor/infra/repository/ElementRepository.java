package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.entity.Element;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElementRepository extends JpaRepository<Element, UUID> {

    List<Element> findAllByProjectId(UUID projectId);

    /** Busca restrita ao projeto: elemento de outro projeto e tratado como inexistente. */
    Optional<Element> findByIdAndProjectId(UUID id, UUID projectId);

    /** RF10: submete todos os elementos em rascunho do projeto; devolve quantos foram submetidos. */
    @Modifying
    @Query(value = """
            UPDATE elements SET submission_status = 'SUBMITTED'
             WHERE project_id = :projectId AND deleted_at IS NULL AND submission_status = 'DRAFT'
            """, nativeQuery = true)
    int submitDrafts(@Param("projectId") UUID projectId);
}
