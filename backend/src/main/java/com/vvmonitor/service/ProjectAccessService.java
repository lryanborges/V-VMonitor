package com.vvmonitor.service;

import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.domain.exception.ProjectNotFoundException;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ponto unico de controle de acesso aos projetos (RNF1, RNF4).
 * Toda operacao dentro de um projeto deve passar por aqui antes de ler ou alterar dados.
 */
@Service
public class ProjectAccessService {

    private final ProjectMemberRepository memberRepository;

    public ProjectAccessService(ProjectMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * Garante que o usuario e membro do projeto. Quem nao e membro recebe 404, e nao 403,
     * para nao confirmar que o projeto existe.
     */
    @Transactional(readOnly = true)
    public ProjectMember requireMember(UUID projectId, UUID userId) {
        return memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    /** Garante que o usuario e membro com pelo menos a permissao pedida (ex.: EDITOR para alterar). */
    @Transactional(readOnly = true)
    public ProjectMember requireRole(UUID projectId, UUID userId, MemberRole required) {
        ProjectMember member = requireMember(projectId, userId);
        if (!member.getRole().includes(required)) {
            throw new ProjectAccessDeniedException(required);
        }
        return member;
    }
}
