package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.entity.TestCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TestCoverageRepository extends JpaRepository<TestCoverage, UUID> {

    List<TestCoverage> findAllByTestId(UUID testId);

    /** Vinculos de todos os testes de um projeto (o filtro por projeto vem dos testes). */
    @Query("""
            select c from TestCoverage c
            where c.testId in (select t.id from TestCase t where t.projectId = :projectId)
            """)
    List<TestCoverage> findAllOfProject(@Param("projectId") UUID projectId);
}
