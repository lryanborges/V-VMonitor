package com.vvmonitor.domain.version;

/** Quantos itens (elementos e relacoes somados) foram adicionados, alterados e removidos. */
public record ChangeSummary(int added, int modified, int removed) {

    public int total() {
        return added + modified + removed;
    }
}
