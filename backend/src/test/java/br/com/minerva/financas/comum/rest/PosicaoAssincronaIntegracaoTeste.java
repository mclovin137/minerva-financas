package br.com.minerva.financas.comum.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opção B: ciclo de vida da execução assíncrona. TC-073 a TC-076 e as decisões D-C3.
 * <p>
 * Cada caso roda como um usuário próprio. O motivo é concreto: uma execução criada e nunca
 * consultada continua retida até expirar, e o limite de execuções por usuário é justamente o que
 * este arquivo testa — casos compartilhando um login se envenenariam mutuamente com 429.
 */
class PosicaoAssincronaIntegracaoTeste extends TesteIntegracao {

    private static final String DIA = "2020-02-28";

    @Autowired
    private JdbcTemplate db;

    private String prepararComPosicao(String caso) {
        String codigo = codigoDeAtivo(caso);
        criarAtivo(codigo);
        definirPreco(codigo, "2020-01-02", "10.00");
        credito("1000.00", "2020-01-02");
        comprar(codigo, DIA, "2.00", "20.00");
        return codigo;
    }

    /** Executa o caso inteiro sob um login exclusivo, isolando o limite de execuções retidas. */
    private void comoUsuario(int indice, Runnable caso) {
        como("usuario" + indice, "senha" + indice, caso);
    }

    /** Espera a execução sair de "em andamento", sem mascarar um 425 eterno como sucesso. */
    private Resposta aguardarConclusao(long id) {
        Instant limite = Instant.now().plus(Duration.ofSeconds(20));
        while (Instant.now().isBefore(limite)) {
            Resposta resposta = get("/posicao/" + id);
            if (resposta.status() != 425) {
                return resposta;
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("a execução " + id + " continuou em 425 além do limite de espera");
    }

    /** Insere movimentações direto no banco: o objetivo é dar trabalho à consulta, não exercitar o POST. */
    private void carregarMovimentacoesEmMassa(String login, int total) {
        Long usuario = db.queryForObject("SELECT id FROM usuario WHERE login = ?", Long.class, login);
        List<Long> ativos = db.queryForList(
                "SELECT id FROM ativo WHERE codigo LIKE 'ATIVO%' ORDER BY id", Long.class);

        List<Object[]> lote = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            lote.add(new Object[]{usuario, ativos.get(i % ativos.size()), DIA, "COMPRA", 100L, 1000L});
        }
        db.batchUpdate("""
                INSERT INTO movimentacao (usuario_id, ativo_id, data, tipo, quantidade_e2, valor_centavos)
                VALUES (?, ?, ?, ?, ?, ?)
                """, lote);
    }

    private long solicitar() {
        Resposta criacao = get("/posicao?data=" + DIA);
        assertEquals(202, criacao.status());
        return criacao.json().get("id").asLong();
    }

    @Test
    @DisplayName("TC-073 criação assíncrona retorna 202 e um identificador de execução")
    void tc073() {
        comoUsuario(1, () -> {
            prepararComPosicao("TC-073");

            Resposta resposta = get("/posicao?data=" + DIA);

            assertEquals(202, resposta.status());
            assertTrue(resposta.json().get("id").asLong() > 0, "o identificador é devolvido de imediato");
        });
    }

    @Test
    @DisplayName("TC-074 consulta pendente retorna 425 imediatamente, sem bloquear")
    void tc074() {
        // Carga grande de propósito: com poucas movimentações a execução termina antes da primeira
        // consulta e o caso passaria sem nunca observar um 425 — que é exatamente o que ele existe
        // para provar.
        carregarMovimentacoesEmMassa("usuario2", 120_000);

        comoUsuario(2, () -> {
            long id = solicitar();

            Instant antes = Instant.now();
            Resposta resposta = get("/posicao/" + id);
            Duration decorrido = Duration.between(antes, Instant.now());

            assertEquals(425, resposta.status(),
                    "enquanto a execução não termina, a consulta responde 425");
            assertTrue(decorrido.toMillis() < 2_000,
                    "e responde de imediato, sem esperar a conclusão; levou " + decorrido.toMillis() + " ms");

            assertEquals(200, aguardarConclusao(id).status(), "e depois conclui normalmente");
        });
    }

    @Test
    @DisplayName("TC-075 execução concluída retorna 200 com a posição")
    void tc075() {
        comoUsuario(3, () -> {
            String codigo = prepararComPosicao("TC-075");
            long id = solicitar();

            Resposta resposta = aguardarConclusao(id);

            assertEquals(200, resposta.status());
            assertTrue(resposta.corpo().contains(codigo), "a posição do ativo movimentado está no corpo");
            assertNumero("2.00", resposta.item(0).get("quantidade"), "quantidade agregada");
            assertNumero("20.00", resposta.item(0).get("valorMercadoTotal"), "2,00 × 10,00");
        });
    }

    @Test
    @DisplayName("TC-076 identificador inexistente ou já entregue retorna 404")
    void tc076() {
        comoUsuario(4, () -> {
            prepararComPosicao("TC-076");
            long id = solicitar();
            assertEquals(200, aguardarConclusao(id).status());

            Resposta segunda = get("/posicao/" + id);
            assertEquals(404, segunda.status(), "o conteúdo é descartado após a entrega");
            assertEquals("RECURSO_NAO_ENCONTRADO", segunda.codigoDeErro());

            assertEquals(404, get("/posicao/999999").status(), "identificador que nunca existiu");
        });
    }

    @Test
    @DisplayName("D-C3 a execução de outro usuário é indistinguível de inexistente")
    void isolamentoEntreUsuarios() {
        String codigo = codigoDeAtivo("TC-077");
        criarAtivo(codigo);
        definirPreco(codigo, "2020-01-02", "10.00");

        long[] id = new long[1];
        como("usuario8", "senha8", () -> {
            credito("1000.00", "2020-01-02");
            comprar(codigo, DIA, "2.00", "20.00");
            id[0] = solicitar();
        });

        como("usuario9", "senha9", () -> {
            Resposta resposta = get("/posicao/" + id[0]);
            assertEquals(404, resposta.status(),
                    "um 403 aqui confirmaria que o identificador existe e viraria oráculo de enumeração");
        });

        como("usuario8", "senha8", () -> assertEquals(200, aguardarConclusao(id[0]).status(),
                "e o dono continua conseguindo buscar o próprio resultado"));
    }

    @Test
    @DisplayName("D-C3 o limite de execuções retidas por usuário é aplicado com 429")
    void backpressure() {
        comoUsuario(5, () -> backpressureComoUsuarioExclusivo());
    }

    private void backpressureComoUsuarioExclusivo() {
        prepararComPosicao("DC3");
        final int limite = 4;

        // As quatro primeiras são aceitas e ficam retidas: nenhuma é consultada, então nenhuma é
        // entregue nem liberada. A quinta tem de ser recusada.
        for (int i = 1; i <= limite; i++) {
            assertEquals(202, get("/posicao?data=" + DIA).status(),
                    "a execução " + i + " cabe no limite de " + limite);
        }

        Resposta excedente = get("/posicao?data=" + DIA);
        assertEquals(429, excedente.status(), "a quinta execução retida é recusada");
        assertEquals("EXCESSO_DE_EXECUCOES", excedente.codigoDeErro());
    }

    @Test
    @DisplayName("D-C3 consumir um resultado libera espaço para uma nova execução")
    void backpressureLiberaAoEntregar() {
        comoUsuario(6, () -> liberacaoComoUsuarioExclusivo());
    }

    private void liberacaoComoUsuarioExclusivo() {
        prepararComPosicao("DC3b");
        long primeira = solicitar();
        for (int i = 0; i < 3; i++) {
            assertEquals(202, get("/posicao?data=" + DIA).status());
        }
        assertEquals(429, get("/posicao?data=" + DIA).status(), "o limite está saturado");

        assertEquals(200, aguardarConclusao(primeira).status(), "consumir entrega e descarta");

        assertEquals(202, get("/posicao?data=" + DIA).status(),
                "com uma vaga livre, uma nova execução é aceita");
    }
}
