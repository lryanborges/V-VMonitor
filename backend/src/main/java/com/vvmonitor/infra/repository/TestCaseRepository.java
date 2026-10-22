package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    List<TestCase> findAllByProjectId(UUID projectId);

    /** Busca restrita ao projeto: teste de outro projeto e tratado como inexistente. */
    Optional<TestCase> findByIdAndProjectId(UUID id, UUID projectId);
}
