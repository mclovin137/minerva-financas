package br.com.minerva.financas.comum.rest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TesteIntegracaoTeste {

    @Test
    void identificaTodosOsCasosDeclaradosNoDisplayName() {
        assertEquals(List.of("TC-078", "TC-079", "TC-080"),
                TesteIntegracao.identificadoresDeCaso(
                        "TC-078/079/080 processa 200.000 movimentações com heap estável"));
    }

    @Test
    void identificaCasoUnico() {
        assertEquals(List.of("TC-042"),
                TesteIntegracao.identificadoresDeCaso("TC-042 consulta a posição"));
    }

    @Test
    void usaFallbackQuandoDisplayNameNaoDeclaraCaso() {
        assertEquals(List.of("sem-tc"),
                TesteIntegracao.identificadoresDeCaso("consulta a posição sem identificador"));
    }
}
