package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.enums.ElementKind;

/**
 * Previa do codigo que o proximo elemento do tipo deve receber. Nao reserva o numero: se outro membro
 * cadastrar antes, o codigo final sera o seguinte.
 */
public record NextCodeResponse(ElementKind kind, String code) {
}
