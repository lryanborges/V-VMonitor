package com.vvmonitor.domain.version;

import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.Priority;
import com.vvmonitor.domain.version.ModelSnapshot.ElementSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.RelationshipSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.TypeSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ModelChangesTest {

    private final UUID dependencia = UUID.randomUUID();
    private final UUID refinamento = UUID.randomUUID();
    private final ElementSnapshot rf1 = element("RF1", "Cadastro", Priority.MANDATORY);
    private final ElementSnapshot rf2 = element("RF2", "Login", Priority.MANDATORY);
    private final RelationshipSnapshot rf2DependeRf1 =
            new RelationshipSnapshot(UUID.randomUUID(), rf2.id(), rf1.id(), dependencia);

    private static ElementSnapshot element(String code, String description, Priority priority) {
        return new ElementSnapshot(UUID.randomUUID(), code, ElementKind.FUNCTIONAL, description, priority);
    }

    private ModelSnapshot snapshot(List<ElementSnapshot> elements, List<RelationshipSnapshot> relationships) {
        return new ModelSnapshot(ModelSnapshot.FORMAT, elements, relationships, List.of(
                new TypeSnapshot(dependencia, "Dependência", false, false),
                new TypeSnapshot(refinamento, "Refinamento", false, false)), null);
    }

    @Test
    void antesDaPrimeiraVersaoTudoEAdicionado() {
        ModelChanges changes = ModelChanges.between(ModelSnapshot.EMPTY, snapshot(List.of(rf1, rf2), List.of(rf2DependeRf1)));

        assertThat(changes.addedElements()).containsExactly(rf1, rf2);
        assertThat(changes.addedRelationships()).containsExactly(rf2DependeRf1);
        assertThat(changes.summary()).isEqualTo(new ChangeSummary(3, 0, 0));
        assertThat(changes.pendingIds()).containsExactlyInAnyOrder(rf1.id(), rf2.id(), rf2DependeRf1.id());
    }

    @Test
    void modeloIgualAUltimaVersaoNaoTemMudancas() {
        ModelSnapshot model = snapshot(List.of(rf1, rf2), List.of(rf2DependeRf1));

        ModelChanges changes = ModelChanges.between(model, snapshot(List.of(rf1, rf2), List.of(rf2DependeRf1)));

        assertThat(changes.isEmpty()).isTrue();
        assertThat(changes.pendingIds()).isEmpty();
    }

    @Test
    void detectaElementoAlteradoComAntesEDepois() {
        ElementSnapshot rf2Editado = new ElementSnapshot(rf2.id(), "RF2", ElementKind.FUNCTIONAL, "Login com e-mail",
                Priority.DESIRABLE);

        ModelChanges changes = ModelChanges.between(snapshot(List.of(rf1, rf2), List.of()),
                snapshot(List.of(rf1, rf2Editado), List.of()));

        assertThat(changes.modifiedElements()).singleElement().satisfies(m -> {
            assertThat(m.before().description()).isEqualTo("Login");
            assertThat(m.after().description()).isEqualTo("Login com e-mail");
        });
        assertThat(changes.summary()).isEqualTo(new ChangeSummary(0, 1, 0));
        assertThat(changes.pendingIds()).containsExactly(rf2.id());
    }

    @Test
    void editarEVoltarAoOriginalNaoContaComoAlteracao() {
        ElementSnapshot rf2Igual = new ElementSnapshot(rf2.id(), "RF2", ElementKind.FUNCTIONAL, "Login",
                Priority.MANDATORY);

        ModelChanges changes = ModelChanges.between(snapshot(List.of(rf2), List.of()), snapshot(List.of(rf2Igual), List.of()));

        assertThat(changes.isEmpty()).isTrue();
    }

    @Test
    void detectaRemocoesQueNaoAparecemNoModeloAtual() {
        ModelChanges changes = ModelChanges.between(snapshot(List.of(rf1, rf2), List.of(rf2DependeRf1)),
                snapshot(List.of(rf1), List.of()));

        assertThat(changes.removedElements()).containsExactly(rf2);
        assertThat(changes.removedRelationships()).containsExactly(rf2DependeRf1);
        assertThat(changes.summary()).isEqualTo(new ChangeSummary(0, 0, 2));
        // removidos nao estao no modelo atual: nao ha o que marcar como rascunho
        assertThat(changes.pendingIds()).isEmpty();
    }

    @Test
    void relacaoComTipoOuDirecaoTrocadosEAlterada() {
        RelationshipSnapshot trocouTipo = new RelationshipSnapshot(rf2DependeRf1.id(), rf2.id(), rf1.id(), refinamento);
        RelationshipSnapshot inverteu = new RelationshipSnapshot(rf2DependeRf1.id(), rf1.id(), rf2.id(), dependencia);
        ModelSnapshot base = snapshot(List.of(rf1, rf2), List.of(rf2DependeRf1));

        assertThat(ModelChanges.between(base, snapshot(List.of(rf1, rf2), List.of(trocouTipo))).modifiedRelationships())
                .singleElement().satisfies(m -> assertThat(m.after().typeId()).isEqualTo(refinamento));
        assertThat(ModelChanges.between(base, snapshot(List.of(rf1, rf2), List.of(inverteu))).modifiedRelationships())
                .singleElement().satisfies(m -> assertThat(m.after().sourceId()).isEqualTo(rf1.id()));
    }
}
