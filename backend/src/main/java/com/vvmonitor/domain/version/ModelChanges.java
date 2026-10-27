package com.vvmonitor.domain.version;

import com.vvmonitor.domain.version.ModelSnapshot.ElementSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.RelationshipSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.TestSnapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Diferenca entre duas copias do modelo (base = ultima versao, current = modelo atual), comparando os itens
 * pelo id. Elemento alterado: descricao ou prioridade diferentes (tipo e codigo sao fixos). Relacao alterada:
 * tipo ou direcao diferentes. Teste alterado: descricao ou requisitos cobertos diferentes (inclusive quando um
 * requisito coberto e excluido). Editar e voltar ao conteudo original nao conta como alteracao.
 */
public record ModelChanges(
        List<ElementSnapshot> addedElements,
        List<Modified<ElementSnapshot>> modifiedElements,
        List<ElementSnapshot> removedElements,
        List<RelationshipSnapshot> addedRelationships,
        List<Modified<RelationshipSnapshot>> modifiedRelationships,
        List<RelationshipSnapshot> removedRelationships,
        List<TestSnapshot> addedTests,
        List<Modified<TestSnapshot>> modifiedTests,
        List<TestSnapshot> removedTests
) {

    /** O mesmo item antes (na base) e depois (no modelo atual). */
    public record Modified<T>(T before, T after) {
    }

    public static ModelChanges between(ModelSnapshot base, ModelSnapshot current) {
        Map<UUID, ElementSnapshot> oldElements = base.elementsById();
        Map<UUID, ElementSnapshot> newElements = current.elementsById();
        Map<UUID, RelationshipSnapshot> oldRelationships = base.relationshipsById();
        Map<UUID, RelationshipSnapshot> newRelationships = current.relationshipsById();
        Map<UUID, TestSnapshot> oldTests = base.testsById();
        Map<UUID, TestSnapshot> newTests = current.testsById();
        return new ModelChanges(
                missingFrom(current.elements(), oldElements, ElementSnapshot::id),
                modified(current.elements(), oldElements, ElementSnapshot::id, ModelChanges::sameElement),
                missingFrom(base.elements(), newElements, ElementSnapshot::id),
                missingFrom(current.relationships(), oldRelationships, RelationshipSnapshot::id),
                modified(current.relationships(), oldRelationships, RelationshipSnapshot::id, Objects::equals),
                missingFrom(base.relationships(), newRelationships, RelationshipSnapshot::id),
                missingFrom(current.tests(), oldTests, TestSnapshot::id),
                modified(current.tests(), oldTests, TestSnapshot::id, ModelChanges::sameTest),
                missingFrom(base.tests(), newTests, TestSnapshot::id));
    }

    public boolean isEmpty() {
        return summary().total() == 0;
    }

    public ChangeSummary summary() {
        return new ChangeSummary(
                addedElements.size() + addedRelationships.size() + addedTests.size(),
                modifiedElements.size() + modifiedRelationships.size() + modifiedTests.size(),
                removedElements.size() + removedRelationships.size() + removedTests.size());
    }

    /** Ids dos itens do modelo atual que ainda nao estao na ultima versao como estao agora (rascunhos). */
    public Set<UUID> pendingIds() {
        Set<UUID> ids = new HashSet<>();
        addedElements.forEach(e -> ids.add(e.id()));
        modifiedElements.forEach(m -> ids.add(m.after().id()));
        addedRelationships.forEach(r -> ids.add(r.id()));
        modifiedRelationships.forEach(m -> ids.add(m.after().id()));
        addedTests.forEach(t -> ids.add(t.id()));
        modifiedTests.forEach(m -> ids.add(m.after().id()));
        return ids;
    }

    /** Itens de "items" cujo id nao existe em "others": adicionados (atual x base) ou removidos (base x atual). */
    private static <T> List<T> missingFrom(List<T> items, Map<UUID, T> others, Function<T, UUID> id) {
        return items.stream().filter(item -> !others.containsKey(id.apply(item))).toList();
    }

    private static <T> List<Modified<T>> modified(List<T> current, Map<UUID, T> base, Function<T, UUID> id,
                                                  BiPredicate<T, T> same) {
        return current.stream()
                .flatMap(after -> {
                    T before = base.get(id.apply(after));
                    return before == null || same.test(before, after) ? Stream.empty()
                            : Stream.of(new Modified<>(before, after));
                })
                .toList();
    }

    private static boolean sameTest(TestSnapshot a, TestSnapshot b) {
        return Objects.equals(a.description(), b.description()) && Objects.equals(a.elementIds(), b.elementIds());
    }

    private static boolean sameElement(ElementSnapshot a, ElementSnapshot b) {
        return Objects.equals(a.description(), b.description()) && a.priority() == b.priority();
    }
}
