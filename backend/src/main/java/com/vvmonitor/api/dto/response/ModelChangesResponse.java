package com.vvmonitor.api.dto.response;

import com.vvmonitor.api.dto.response.VersionResponse.VersionedRelationship;
import com.vvmonitor.domain.version.ChangeSummary;
import com.vvmonitor.domain.version.ModelChanges;
import com.vvmonitor.domain.version.ModelChanges.Modified;
import com.vvmonitor.domain.version.ModelSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.ElementSnapshot;

import java.util.List;

/**
 * Mudancas entre duas copias do modelo: de baseVersion (null = antes da primeira versao) para targetVersion
 * (null = modelo atual). Sem parametros, e o que mudou desde a ultima versao, ou seja, o que entra na proxima
 * submissao. Relacoes vem com as pontas e o tipo resolvidos: as removidas pela copia de origem, as demais
 * pela de destino, para exibir mesmo quando o elemento da ponta tambem foi removido.
 */
public record ModelChangesResponse(
        Integer baseVersion,
        Integer targetVersion,
        ChangeSummary summary,
        Group<ElementSnapshot> elements,
        Group<VersionedRelationship> relationships
) {

    public record Group<T>(List<T> added, List<Modified<T>> modified, List<T> removed) {
    }

    public static ModelChangesResponse from(Integer baseVersion, Integer targetVersion, ModelSnapshot base,
                                            ModelSnapshot current, ModelChanges changes) {
        SnapshotIndex before = new SnapshotIndex(base);
        SnapshotIndex now = new SnapshotIndex(current);
        return new ModelChangesResponse(
                baseVersion,
                targetVersion,
                changes.summary(),
                new Group<>(changes.addedElements(), changes.modifiedElements(), changes.removedElements()),
                new Group<>(
                        changes.addedRelationships().stream().map(now::relationship).toList(),
                        changes.modifiedRelationships().stream()
                                .map(m -> new Modified<>(before.relationship(m.before()), now.relationship(m.after())))
                                .toList(),
                        changes.removedRelationships().stream().map(before::relationship).toList()));
    }
}
