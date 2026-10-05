package com.vvmonitor.domain.exception;

import java.util.UUID;

/**
 * Projeto inexistente ou sem acesso para o usuario. Os dois casos tem a mesma resposta
 * para nao revelar a existencia de projetos alheios (RNF4).
 */
public class ProjectNotFoundException extends ResourceNotFoundException {

    public ProjectNotFoundException(UUID id) {
        super("Projeto " + id + " não encontrado.");
    }
}
