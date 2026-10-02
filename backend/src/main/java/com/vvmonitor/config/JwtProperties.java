package com.vvmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Propriedades jwt.* do application.yml; a aplicacao nao sobe com segredo ausente ou curto. */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, Duration expiration) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret (variavel JWT_SECRET) deve ter pelo menos " + MIN_SECRET_BYTES + " bytes.");
        }
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalStateException("jwt.expiration (variavel JWT_EXPIRATION) deve ser positivo.");
        }
    }
}
