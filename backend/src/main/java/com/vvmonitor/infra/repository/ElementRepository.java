package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.entity.Element;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElementRepository extends JpaRepository<Element, UUID> {

    List<Element> findAllByProjectId(UUID projectId);

    /** Busca restrita ao projeto: elemento de outro projeto e tratado como inexistente. */
    Optional<Element> findByIdAndProjectId(UUID id, UUID projectId);
}
