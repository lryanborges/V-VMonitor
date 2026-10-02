package com.vvmonitor.domain.exception;

/**
 * UC-02, excecao do passo 4. A mesma mensagem e usada para e-mail inexistente e senha errada,
 * para nao revelar quais e-mails estao cadastrados.
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("E-mail ou senha inválidos.");
    }
}
