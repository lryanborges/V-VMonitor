package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.AddMemberRequest;
import com.vvmonitor.api.dto.response.MemberResponse;
import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.entity.User;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.AlreadyMemberException;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.MemberNotFoundException;
import com.vvmonitor.domain.exception.OwnerCannotBeRemovedException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Compartilhamento do projeto (RF20, RNF4). Regras definidas com o usuario (sem caso de uso no documento):
 * so o proprietario adiciona pessoas, sempre como editor, e a pessoa ja precisa ter conta; o proprietario
 * remove qualquer editor e cada pessoa pode sair do projeto; o proprietario nao pode ser removido.
 */
@Service
public class MemberService {

    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService accessService;

    public MemberService(ProjectMemberRepository memberRepository, UserRepository userRepository,
                         ProjectRepository projectRepository, ProjectAccessService accessService) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
    }

    /** Pessoas com acesso, o proprietario primeiro. */
    @Transactional(readOnly = true)
    public List<MemberResponse> list(UUID projectId, UUID userId) {
        accessService.requireMember(projectId, userId);
        return memberRepository.findDetailsOf(projectId).stream().map(MemberResponse::from).toList();
    }

    /** RF20: adiciona como editor a pessoa cadastrada com o e-mail informado. */
    @Transactional
    public MemberResponse add(UUID projectId, UUID userId, AddMemberRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.OWNER);
        String email = User.normalizeEmail(request.email());
        User user = userRepository.findByEmail(email).orElseThrow(() -> new InvalidFieldException("email",
                "Este e-mail não tem cadastro no ReqTrace Monitor. Peça para a pessoa criar uma conta e adicione de novo."));
        if (memberRepository.findByProjectIdAndUserId(projectId, user.getId()).isPresent()) {
            throw new AlreadyMemberException(email);
        }
        ProjectMember member;
        try {
            member = memberRepository.saveAndFlush(new ProjectMember(projectId, user.getId(), MemberRole.EDITOR));
        } catch (DataIntegrityViolationException e) {
            // duas adicoes simultaneas da mesma pessoa; o indice unico do banco barra a segunda
            throw new AlreadyMemberException(email);
        }
        projectRepository.touch(projectId);
        return new MemberResponse(user.getId(), user.getName(), user.getEmail(), member.getRole(), member.getAddedAt());
    }

    /**
     * RF20: remove o acesso de uma pessoa. Pode o proprietario (qualquer editor) ou a propria pessoa (sair do
     * projeto). O proprietario nao pode ser removido.
     */
    @Transactional
    public void remove(UUID projectId, UUID userId, UUID memberUserId) {
        ProjectMember actor = accessService.requireMember(projectId, userId);
        ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, memberUserId)
                .orElseThrow(() -> new MemberNotFoundException(memberUserId));
        if (member.getRole() == MemberRole.OWNER) {
            throw new OwnerCannotBeRemovedException();
        }
        boolean self = memberUserId.equals(userId);
        if (!self && actor.getRole() != MemberRole.OWNER) {
            throw new ProjectAccessDeniedException(MemberRole.OWNER);
        }
        memberRepository.delete(member);
        projectRepository.touch(projectId);
    }
}
