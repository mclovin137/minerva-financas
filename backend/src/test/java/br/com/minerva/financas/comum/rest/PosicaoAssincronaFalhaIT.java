package br.com.minerva.financas.comum.rest;

import br.com.minerva.financas.comum.dominio.PrecoUnitario;
import br.com.minerva.financas.movimentacao.dao.ILeitorDeMovimentacoes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * TC-077: uma execução assíncrona que falha responde 500, e <strong>nunca</strong> 425.
 * <p>
 * Mascarar falha como "ainda processando" é um falso verde observável pelo cliente: ele ficaria
 * repetindo a consulta para sempre, esperando um resultado que não vai existir.
 * <p>
 * A falha é forçada substituindo a porta de leitura por uma que lança. Fazer o teste depender de uma
 * falha real de banco seria não determinístico; o que importa provar é o mapeamento de estado, e ele
 * é o mesmo qualquer que seja a origem do erro.
 */
@Import(PosicaoAssincronaFalhaIT.LeituraQueFalha.class)
class PosicaoAssincronaFalhaIT extends TesteIntegracao {

    private static final String DIA = "2020-02-28";

    @TestConfiguration
    static class LeituraQueFalha {
        @Bean
        @Primary
        ILeitorDeMovimentacoes leitorQueFalha() {
            return new ILeitorDeMovimentacoes() {
                @Override
                public void percorrer(long usuario, LocalDate data, int totalDeParticoes, int particao,
                                      ConsumidorDeMovimento consumidor) {
                    throw new IllegalStateException("falha simulada na leitura de movimentações");
                }

                @Override
                public Map<String, PrecoUnitario> precosVigentes(LocalDate data) {
                    return Map.of();
                }
            };
        }
    }

    @Test
    @DisplayName("TC-077 execução que falha responde 500 e nunca fica presa em 425")
    void tc077() {
        Resposta criacao = get("/posicao?data=" + DIA);
        assertEquals(202, criacao.status(), "a criação continua sendo aceita: a falha é da execução");
        long id = criacao.json().get("id").asLong();

        Resposta resposta = aguardarSaidaDoPendente(id);

        assertEquals(500, resposta.status(), "a falha é reportada como erro do servidor");
        assertEquals("ERRO_INTERNO", resposta.codigoDeErro());
        assertNotEquals(425, resposta.status(),
                "425 significaria progresso e faria o cliente esperar para sempre");
    }

    private Resposta aguardarSaidaDoPendente(long id) {
        Instant limite = Instant.now().plus(Duration.ofSeconds(20));
        while (Instant.now().isBefore(limite)) {
            Resposta resposta = get("/posicao/" + id);
            if (resposta.status() != 425) {
                return resposta;
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("a execução que falhou continuou respondendo 425 — exatamente o defeito que "
                + "este caso existe para impedir");
    }
}
