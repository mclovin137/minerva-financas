package br.com.minerva.financas.comum.rest;

import br.com.minerva.financas.posicao.service.PosicaoAssincronaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Metas de desempenho da opção B: TC-078, TC-079 e TC-080.
 * <p>
 * Reproduzir com {@code ./mvnw -f backend/pom.xml test -Dtest=PosicaoDesempenhoIntegracaoTeste}.
 * <p>
 * A carga é inserida direto no banco, e não pela API: o objetivo é medir a <em>consulta</em> sobre
 * 200.000 movimentações, e passar por 200.000 requisições HTTP mediria outra coisa.
 */
class PosicaoDesempenhoIntegracaoTeste extends TesteIntegracao {

    private static final int MOVIMENTACOES = 200_000;
    private static final long LIMITE_DE_HEAP_BYTES = 64L * 1024 * 1024;
    private static final String DIA = "2020-02-28";

    @Autowired
    private JdbcTemplate db;

    @Autowired
    private PosicaoAssincronaService servico;

    /**
     * Carga determinística: compras distribuídas entre os 128 ativos do seed, de modo que a partição
     * por identificador de ativo tenha trabalho em todas as fatias.
     */
    private void carregar() {
        Long usuario = db.queryForObject("SELECT id FROM usuario WHERE login = 'usuario0'", Long.class);
        List<Long> ativos = db.queryForList(
                "SELECT id FROM ativo WHERE codigo LIKE 'ATIVO%' ORDER BY id", Long.class);
        assertEquals(128, ativos.size(), "o seed do enunciado precisa estar carregado");

        List<Object[]> lote = new ArrayList<>(MOVIMENTACOES);
        for (int i = 0; i < MOVIMENTACOES; i++) {
            lote.add(new Object[]{usuario, ativos.get(i % ativos.size()), DIA, "COMPRA", 100L, 1000L});
        }
        db.batchUpdate("""
                INSERT INTO movimentacao (usuario_id, ativo_id, data, tipo, quantidade_e2, valor_centavos)
                VALUES (?, ?, ?, ?, ?, ?)
                """, lote);

        Integer total = db.queryForObject("SELECT COUNT(*) FROM movimentacao", Integer.class);
        assertEquals(MOVIMENTACOES, total, "a fixture de carga foi persistida integralmente");
    }

    private Resposta executarPosicaoAssincrona() {
        Resposta criacao = get("/posicao?data=" + DIA);
        assertEquals(202, criacao.status());
        long id = criacao.json().get("id").asLong();

        Instant limite = Instant.now().plus(Duration.ofMinutes(3));
        while (Instant.now().isBefore(limite)) {
            Resposta resposta = get("/posicao/" + id);
            if (resposta.status() != 425) {
                return resposta;
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("a execução não concluiu dentro do limite");
    }

    private static long heapUsado() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    @Test
    @DisplayName("TC-078/079/080 processa 200.000 movimentações com heap estável e paralelismo observável")
    void desempenhoDaPosicaoAssincrona() {
        carregar();

        // O GC é sugerido antes da medição para reduzir o ruído de lixo já acumulado pela carga.
        // Sem isso a diferença mediria o histórico do processo, não o custo da consulta.
        System.gc();
        long heapAntes = heapUsado();
        Instant inicio = Instant.now();

        Resposta resposta = executarPosicaoAssincrona();

        Duration duracao = Duration.between(inicio, Instant.now());
        long heapDepois = heapUsado();
        long variacao = Math.abs(heapDepois - heapAntes);
        Set<String> threads = servico.threadsDaUltimaExecucao();

        System.out.printf("""
                        [TC-078] movimentações processadas: %d
                        [TC-078] duração da consulta: %d ms
                        [TC-079] heap antes:  %d bytes (%.1f MB)
                        [TC-079] heap depois: %d bytes (%.1f MB)
                        [TC-079] variação:    %d bytes (%.1f MB), limite 64 MB
                        [TC-080] threads de agregação: %s
                        %n""",
                MOVIMENTACOES, duracao.toMillis(),
                heapAntes, heapAntes / 1048576.0,
                heapDepois, heapDepois / 1048576.0,
                variacao, variacao / 1048576.0,
                threads);

        assertEquals(200, resposta.status(), "TC-078: a consulta conclui sobre 200.000 movimentações");
        assertEquals(128, resposta.json().size(), "TC-078: uma linha por ativo movimentado");

        assertTrue(variacao < LIMITE_DE_HEAP_BYTES,
                "TC-079: a variação de heap foi de " + variacao + " bytes, acima do limite de 64 MB");

        assertTrue(threads.size() >= 2,
                "TC-080: esperadas ao menos duas threads de agregação distintas, observadas " + threads);
        assertTrue(threads.stream().allMatch(nome -> nome.startsWith("posicao-worker-")),
                "TC-080: a agregação roda no pool dedicado, não no pool do servidor HTTP: " + threads);
    }
}
