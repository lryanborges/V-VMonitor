package com.vvmonitor.domain.enums;

/**
 * Situacao de um elemento ou relacao em relacao a ultima versao do modelo (RF10 + RF17), calculada pela
 * comparacao com ela (nao e gravada). DRAFT: adicionado ou alterado desde a ultima versao (pendente).
 * SUBMITTED: igual ao que esta na ultima versao.
 */
public enum SubmissionStatus {
    DRAFT,
    SUBMITTED;

    public static SubmissionStatus of(boolean pending) {
        return pending ? DRAFT : SUBMITTED;
    }
}
