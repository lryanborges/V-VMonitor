package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.RegisterUserRequest;
import com.vvmonitor.api.dto.response.UserResponse;
import com.vvmonitor.domain.entity.User;
import com.vvmonitor.domain.exception.EmailAlreadyRegisteredException;
import com.vvmonitor.domain.exception.UserNotFoundException;
import com.vvmonitor.infra.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** UC-01, passos 4 e 5: valida unicidade do e-mail, gera o hash da senha e armazena a conta. */
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        User user = new User(request.name(), email, passwordEncoder.encode(request.password()));
        try {
            // flush imediato para que uma corrida entre dois cadastros caia aqui, e nao no commit
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException(email);
        }
    }

    /** Usuarios ativos ordenados por nome; removidos (soft delete) sao filtrados pelo @SQLRestriction. */
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("name")).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
