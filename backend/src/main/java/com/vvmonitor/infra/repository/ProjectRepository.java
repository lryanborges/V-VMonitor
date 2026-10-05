package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /** Projetos em que o usuario e membro (inclusive dono), mais recentes primeiro (RF3). */
    @Query("""
            select p from Project p
            where p.id in (select m.projectId from ProjectMember m where m.userId = :userId)
            order by p.updatedAt desc
            """)
    List<Project> findAccessibleBy(@Param("userId") UUID userId);

    /** Marca o projeto como alterado agora (ex.: elemento cadastrado), refletindo em "Atualizado ha...". */
    @Modifying
    @Query(value = "UPDATE projects SET updated_at = now() WHERE id = :id", nativeQuery = true)
    void touch(@Param("id") UUID id);
}
