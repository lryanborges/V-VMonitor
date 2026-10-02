package com.vvmonitor.infra.security;

import com.vvmonitor.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-32-bytes-123456";
    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");

    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofHours(2));

    private JwtService at(Instant instant) {
        return new JwtService(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void tokenEmitidoEValidadoDevolveOUsuario() {
        UUID id = UUID.randomUUID();
        JwtService.IssuedToken issued = at(NOW).issue(id, "ana@exemplo.com");

        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(2)));
        assertThat(at(NOW).parse(issued.token()))
                .contains(new AuthenticatedUser(id, "ana@exemplo.com"));
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        String token = at(NOW).issue(UUID.randomUUID(), "ana@exemplo.com").token();

        assertThat(at(NOW.plus(Duration.ofHours(2)).plusSeconds(1)).parse(token)).isEmpty();
    }

    @Test
    void tokenAdulteradoEhRejeitado() {
        String token = at(NOW).issue(UUID.randomUUID(), "ana@exemplo.com").token();
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(at(NOW).parse(tampered)).isEmpty();
    }

    @Test
    void tokenAssinadoComOutraChaveEhRejeitado() {
        JwtService other = new JwtService(
                new JwtProperties("outro-segredo-tambem-com-mais-de-32-bytes!!", Duration.ofHours(2)),
                Clock.fixed(NOW, ZoneOffset.UTC));
        String token = other.issue(UUID.randomUUID(), "ana@exemplo.com").token();

        assertThat(at(NOW).parse(token)).isEmpty();
    }

    @Test
    void lixoNaoEhToken() {
        assertThat(at(NOW).parse("nao-e-um-jwt")).isEmpty();
        assertThat(at(NOW).parse("")).isEmpty();
    }

    @Test
    void segredoCurtoImpedeASubida() {
        assertThatThrownBy(() -> new JwtProperties("curto", Duration.ofHours(2)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties("", Duration.ofHours(2)))
                .isInstanceOf(IllegalStateException.class);
    }
}
