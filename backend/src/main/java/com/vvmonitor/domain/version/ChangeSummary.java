package com.vvmonitor.domain.version;

/** Quantos itens (elementos, relacoes e testes somados) foram adicionados, alterados e removidos. */
public record ChangeSummary(int added, int modified, int removed) {

    public int total() {
        return added + modified + removed;
    }
}
