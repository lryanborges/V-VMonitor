package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.entity.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RelationshipTypeRepository extends JpaRepository<RelationshipType, UUID> {

    /** Pre-definidos primeiro (na ordem em que foram criados), depois os personalizados do projeto. */
    @Query("""
            select t from RelationshipType t
            where t.projectId is null or t.projectId = :projectId
            order by case when t.projectId is null then 0 else 1 end, t.createdAt, t.name
            """)
    List<RelationshipType> findAvailableFor(@Param("projectId") UUID projectId);

    /** Nome ja usado por um pre-definido ou por um personalizado do projeto (sem diferenciar maiusculas). */
    @Query("""
            select count(t) > 0 from RelationshipType t
            where lower(t.name) = lower(:name) and (t.projectId is null or t.projectId = :projectId)
            """)
    boolean existsNameFor(@Param("projectId") UUID projectId, @Param("name") String name);
}
