package com.vvmonitor.domain.enums;

/** Tipo de elemento do modelo; cada tipo tem prefixo e contador de codigo proprios por projeto. */
public enum ElementKind {
    FUNCTIONAL("RF"),
    NON_FUNCTIONAL("RNF"),
    BUSINESS_RULE("RN");

    private final String prefix;

    ElementKind(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    /** Requisitos (funcionais e nao funcionais) tem prioridade; regras de negocio nao (RF5, RF6). */
    public boolean isRequirement() {
        return this != BUSINESS_RULE;
    }

    public SequenceKind sequenceKind() {
        return SequenceKind.valueOf(name());
    }

    public String code(int number) {
        return prefix + number;
    }
}
