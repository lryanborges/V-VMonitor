package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    /** Membros de varios projetos de uma vez, na ordem em que entraram (o dono primeiro). */
    @Query("""
            select new com.vvmonitor.infra.repository.MemberView(m.projectId, u.id, u.name, m.role)
            from ProjectMember m join User u on u.id = m.userId
            where m.projectId in :projectIds
            order by m.addedAt
            """)
    List<MemberView> findMembersOf(@Param("projectIds") Collection<UUID> projectIds);
}
