package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.enums.ElementKind;

import java.util.UUID;

/** Dados minimos de um elemento para exibi-lo como chip + descricao (relacoes, alertas de exclusao). */
public record ElementSummary(UUID id, String code, ElementKind kind, String description) {

    public static ElementSummary from(Element element) {
        return new ElementSummary(element.getId(), element.getCode(), element.getKind(), element.getDescription());
    }
}
