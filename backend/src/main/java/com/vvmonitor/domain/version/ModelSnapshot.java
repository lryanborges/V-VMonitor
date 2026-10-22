package com.vvmonitor.domain.version;

import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.Relationship;
import com.vvmonitor.domain.entity.RelationshipType;
import com.vvmonitor.domain.entity.TestCase;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.Priority;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copia do modelo de um projeto num instante (RF17): elementos, relacoes, os tipos usados por elas e os testes
 * com os requisitos que cobrem. E o conteudo gravado em project_versions.snapshot (imutavel, RNF2) e tambem a
 * forma de comparar o modelo atual com a ultima versao. format 1 = sem testes (versoes anteriores ao RF9, lidas
 * com a lista de testes vazia); format 2 = com testes. changes resume o que mudou em relacao a versao anterior.
 */
public record ModelSnapshot(
        int format,
        List<ElementSnapshot> elements,
        List<RelationshipSnapshot> relationships,
        List<TypeSnapshot> types,
        List<TestSnapshot> tests,
        ChangeSummary changes
) {

    public static final int FORMAT = 2;

    /** Modelo antes da primeira versao: tudo o que existe conta como adicionado. */
    public static final ModelSnapshot EMPTY = new ModelSnapshot(FORMAT, List.of(), List.of(), List.of(), List.of(), null);

    public ModelSnapshot {
        // versoes de formato 1 nao tem testes
        tests = tests == null ? List.of() : tests;
    }

    public record ElementSnapshot(UUID id, String code, ElementKind kind, String description, Priority priority) {

        static ElementSnapshot of(Element element) {
            return new ElementSnapshot(element.getId(), element.getCode(), element.getKind(), element.getDescription(),
                    element.getPriority());
        }
    }

    public record RelationshipSnapshot(UUID id, UUID sourceId, UUID targetId, UUID typeId) {

        static RelationshipSnapshot of(Relationship relationship) {
            return new RelationshipSnapshot(relationship.getId(), relationship.getSourceId(),
                    relationship.getTargetId(), relationship.getTypeId());
        }
    }

    /** Teste com os requisitos que cobre (ids em ordem, para a comparacao nao depender da ordem de gravacao). */
    public record TestSnapshot(UUID id, String code, String description, List<UUID> elementIds) {
    }

    public record TypeSnapshot(UUID id, String name, boolean symmetric, boolean custom) {

        static TypeSnapshot of(RelationshipType type) {
            return new TypeSnapshot(type.getId(), type.getName(), type.isSymmetric(), type.isCustom());
        }
    }

    /**
     * Monta a copia do modelo atual. Relacoes cuja ponta ou tipo nao existe mais ficam de fora, como na
     * lista de relacoes; so os tipos efetivamente usados entram. Ordem estavel (tipo e numero do codigo;
     * relacoes e tipos por id) para que a mesma situacao gere sempre o mesmo conteudo.
     */
    public static ModelSnapshot of(Collection<Element> elements, Collection<Relationship> relationships,
                                   Map<UUID, RelationshipType> types) {
        return of(elements, relationships, types, List.of(), Map.of());
    }

    /** Com os testes; coverage: id do teste -> requisitos cobertos (os que nao existem mais sao ignorados). */
    public static ModelSnapshot of(Collection<Element> elements, Collection<Relationship> relationships,
                                   Map<UUID, RelationshipType> types, Collection<TestCase> tests,
                                   Map<UUID, List<UUID>> coverage) {
        Map<UUID, Element> byId = elements.stream().collect(Collectors.toMap(Element::getId, Function.identity()));
        List<Relationship> valid = relationships.stream()
                .filter(r -> byId.containsKey(r.getSourceId()) && byId.containsKey(r.getTargetId()))
                .filter(r -> types.containsKey(r.getTypeId()))
                .toList();
        return new ModelSnapshot(
                FORMAT,
                elements.stream()
                        .sorted(Comparator.comparing(Element::getKind).thenComparingInt(Element::codeNumber))
                        .map(ElementSnapshot::of)
                        .toList(),
                valid.stream()
                        .map(RelationshipSnapshot::of)
                        .sorted(Comparator.comparing(RelationshipSnapshot::id))
                        .toList(),
                valid.stream()
                        .map(Relationship::getTypeId)
                        .distinct()
                        .map(types::get)
                        .map(TypeSnapshot::of)
                        .sorted(Comparator.comparing(TypeSnapshot::id))
                        .toList(),
                tests.stream()
                        .sorted(Comparator.comparingInt(TestCase::codeNumber))
                        .map(t -> new TestSnapshot(t.getId(), t.getCode(), t.getDescription(),
                                coverage.getOrDefault(t.getId(), List.of()).stream()
                                        .filter(byId::containsKey)
                                        .distinct()
                                        .sorted()
                                        .toList()))
                        .toList(),
                null);
    }

    /** A mesma copia, com o resumo das mudancas em relacao a versao anterior. */
    public ModelSnapshot withChanges(ChangeSummary summary) {
        return new ModelSnapshot(format, elements, relationships, types, tests, summary);
    }

    public Map<UUID, ElementSnapshot> elementsById() {
        return elements.stream().collect(Collectors.toMap(ElementSnapshot::id, Function.identity()));
    }

    public Map<UUID, RelationshipSnapshot> relationshipsById() {
        return relationships.stream().collect(Collectors.toMap(RelationshipSnapshot::id, Function.identity()));
    }

    public Map<UUID, TestSnapshot> testsById() {
        return tests.stream().collect(Collectors.toMap(TestSnapshot::id, Function.identity()));
    }

    public Map<UUID, TypeSnapshot> typesById() {
        return types.stream().collect(Collectors.toMap(TypeSnapshot::id, Function.identity()));
    }
}
