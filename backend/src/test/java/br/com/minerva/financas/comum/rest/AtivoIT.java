package br.com.minerva.financas.comum.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CRUD de ativos (TC-009 a TC-016) e preço de mercado por data (TC-033 a TC-040). */
class AtivoIT extends TesteIntegracao {

    @Test
    @DisplayName("TC-009 ativo válido retorna 201 sem corpo")
    void tc009() {
        Resposta resposta = criarAtivo(codigoDeAtivo("TC-009"));

        assertEquals(201, resposta.status());
        assertTrue(resposta.corpo().isEmpty());
    }

    @Test
    @DisplayName("TC-010 a listagem contém o ativo criado")
    void tc010() {
        String codigo = codigoDeAtivo("TC-010");
        criarAtivo(codigo);

        Resposta resposta = get("/ativos");

        assertEquals(200, resposta.status());
        assertTrue(resposta.corpo().contains(codigo));
    }

    @Test
    @DisplayName("TC-011 ativo existente retorna 200 com a janela de negociação")
    void tc011() {
        String codigo = codigoDeAtivo("TC-011");
        criarAtivo(codigo, "RF", "2020-01-02", "2021-01-02");

        Resposta resposta = get("/ativos/" + codigo);

        assertEquals(200, resposta.status());
        assertEquals("RF", resposta.json().get("tipo").asString());
        assertEquals("2020-01-02", resposta.json().get("dataEmissao").asString());
        assertEquals("2021-01-02", resposta.json().get("dataVencimento").asString());
    }

    @Test
    @DisplayName("TC-012 alteração válida retorna 200 com o ativo atualizado")
    void tc012() {
        String codigo = codigoDeAtivo("TC-012");
        criarAtivo(codigo);

        Resposta resposta = alterarAtivo(codigo, """
                {"ativo":"%s","nome":"Nome alterado","tipo":"FUNDO",
                 "dataEmissao":"2021-01-04","dataVencimento":"2031-01-04"}""".formatted(codigo));

        assertEquals(200, resposta.status());
        assertEquals("Nome alterado", resposta.json().get("nome").asString());
        assertEquals("FUNDO", resposta.json().get("tipo").asString());
        assertEquals("FUNDO", get("/ativos/" + codigo).json().get("tipo").asString());
    }

    @Test
    @DisplayName("TC-013 remoção válida retorna 204 e o ativo deixa de existir")
    void tc013() {
        String codigo = codigoDeAtivo("TC-013");
        criarAtivo(codigo);

        assertEquals(204, removerAtivo(codigo).status());
        assertEquals(404, get("/ativos/" + codigo).status());
    }

    @Test
    @DisplayName("TC-014 ativo inexistente retorna 404")
    void tc014() {
        Resposta resposta = get("/ativos/NAO-EXISTE-TC014");

        assertEquals(404, resposta.status());
        assertEquals("ATIVO_NAO_ENCONTRADO", resposta.codigoDeErro());
    }

    @Test
    @DisplayName("TC-015 tipo inválido, campo ausente, janela invertida ou precoMercado enviado retornam 400")
    void tc015() {
        String codigo = codigoDeAtivo("TC-015");

        assertEquals(400, criarAtivo(codigo, "CRIPTO", "2020-01-01", "2030-01-01").status(),
                "tipo fora de RV, RF e FUNDO");
        assertEquals(400, post("/ativos",
                "{\"ativo\":\"" + codigo + "\",\"tipo\":\"RV\",\"dataEmissao\":\"2020-01-01\","
                        + "\"dataVencimento\":\"2030-01-01\"}").status(), "nome ausente");
        assertEquals(400, criarAtivo(codigo, "RV", "2030-01-01", "2020-01-01").status(),
                "emissão posterior ao vencimento");
        assertEquals(400, post("/ativos",
                "{\"ativo\":\"" + codigo + "\",\"nome\":\"n\",\"tipo\":\"RV\",\"dataEmissao\":\"2020-01-01\","
                        + "\"dataVencimento\":\"2030-01-01\",\"precoMercado\":10.00}").status(),
                "precoMercado deixou de ser atributo do ativo no nível 2");
    }

    @Test
    @DisplayName("TC-016 duplicidade retorna 409, inclusive sob concorrência, e ativo em uso não é removido")
    void tc016() throws Exception {
        String codigo = codigoDeAtivo("TC-016");

        List<Integer> status = emParalelo(2, () -> criarAtivo(codigo).status());
        assertEquals(1, status.stream().filter(s -> s == 201).count(),
                "exatamente uma criação vence a corrida: " + status);
        assertEquals(1, status.stream().filter(s -> s == 409).count(), "a outra recebe conflito: " + status);

        definirPreco(codigo, "2020-01-02", "10.00");
        credito("100.00", "2020-01-02");
        assertEquals(201, comprar(codigo, "2020-01-02", "1.00", "10.00").status());

        Resposta remocao = removerAtivo(codigo);
        assertEquals(409, remocao.status());
        assertEquals("ATIVO_EM_USO", remocao.codigoDeErro());
    }

    @Test
    @DisplayName("TC-033 definir preço de mercado em uma data retorna 201 na criação e 200 na substituição")
    void tc033() {
        String codigo = codigoDeAtivo("TC-033");
        criarAtivo(codigo);

        Resposta criacao = definirPreco(codigo, "2020-01-02", "105.53");
        Resposta substituicao = definirPreco(codigo, "2020-01-02", "110.00");

        assertEquals(201, criacao.status());
        assertNumero("105.53", criacao.json().get("precoMercado"), "preço criado");
        assertEquals(200, substituicao.status(), "o PUT é idempotente sobre a chave (ativo, data)");
        assertNumero("110.00", substituicao.json().get("precoMercado"), "preço substituído");
    }

    @Test
    @DisplayName("TC-034 a posição usa o preço mais recente com data menor ou igual à consulta")
    void tc034() {
        String codigo = codigoDeAtivo("TC-034");
        criarAtivo(codigo);
        definirPreco(codigo, "2020-01-02", "10.00");
        definirPreco(codigo, "2020-01-10", "20.00");
        definirPreco(codigo, "2020-02-20", "99.00");
        credito("1000.00", "2020-01-02");
        comprar(codigo, "2020-01-02", "1.00", "10.00");

        assertNumero("10.00", precoNaPosicao(codigo, "2020-01-09"), "antes do segundo preço");
        assertNumero("20.00", precoNaPosicao(codigo, "2020-01-10"), "no dia do segundo preço");
        assertNumero("20.00", precoNaPosicao(codigo, "2020-02-19"),
                "um preço futuro nunca vaza para a consulta anterior");
    }

    @Test
    @DisplayName("TC-035 excluir preço histórico existente retorna 204")
    void tc035() {
        String codigo = codigoDeAtivo("TC-035");
        criarAtivo(codigo);
        definirPreco(codigo, "2020-01-02", "10.00");

        assertEquals(204, removerPreco(codigo, "2020-01-02").status());
        assertEquals(404, removerPreco(codigo, "2020-01-02").status(),
                "a segunda exclusão não encontra mais o preço");
    }

    @Test
    @DisplayName("TC-036 emissão anterior ao vencimento é aceita")
    void tc036() {
        assertEquals(201, criarAtivo(codigoDeAtivo("TC-036"), "RV", "2020-01-02", "2020-01-03").status());
    }

    @Test
    @DisplayName("TC-037 emissão igual ou posterior ao vencimento retorna 400")
    void tc037() {
        String codigo = codigoDeAtivo("TC-037");

        Resposta iguais = criarAtivo(codigo, "RV", "2020-01-02", "2020-01-02");
        Resposta invertidas = criarAtivo(codigo, "RV", "2020-01-03", "2020-01-02");

        assertEquals(400, iguais.status());
        assertEquals(400, invertidas.status());
        assertEquals("CAMPO_INVALIDO", iguais.codigoDeErro());
    }

    @Test
    @DisplayName("TC-038 data ou preço inválidos retornam 400")
    void tc038() {
        String codigo = codigoDeAtivo("TC-038");
        criarAtivo(codigo);

        assertEquals(400, comoAdministrador(
                () -> put("/ativos/" + codigo + "/precos/2020-99-99", "{\"precoMercado\":10.00}")).status());
        assertEquals(400, definirPreco(codigo, "2020-01-02", "-1.00").status(), "preço negativo");
        assertEquals(400, definirPreco(codigo, "2020-01-02", "1.000000001").status(), "mais de oito casas");
    }

    @Test
    @DisplayName("TC-039 ativo ou preço inexistente retorna 404")
    void tc039() {
        String codigo = codigoDeAtivo("TC-039");
        criarAtivo(codigo);

        assertEquals(404, definirPreco("NAO-EXISTE-TC039", "2020-01-02", "10.00").status());
        assertEquals(404, removerPreco(codigo, "2020-01-02").status());
    }

    @Test
    @DisplayName("TC-040 preços concorrentes na mesma data não criam estado inconsistente")
    void tc040() throws Exception {
        String codigo = codigoDeAtivo("TC-040");
        criarAtivo(codigo);

        List<Integer> status = emParalelo(4, () -> definirPreco(codigo, "2020-01-02", "10.00").status());

        assertTrue(status.stream().allMatch(s -> s == 200 || s == 201), "nenhuma falha: " + status);
        assertEquals(1, status.stream().filter(s -> s == 201).count(),
                "só a primeira cria; as demais substituem: " + status);
        assertNotEquals(0, get("/ativos/" + codigo).status());
    }

    private tools.jackson.databind.JsonNode precoNaPosicao(String codigo, String data) {
        Resposta resposta = posicao(data);
        return resposta.item(0).get("precoMercado");
    }

    private <T> List<T> emParalelo(int vezes, java.util.concurrent.Callable<T> acao) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(vezes)) {
            List<Future<T>> futuros = new ArrayList<>();
            for (int i = 0; i < vezes; i++) {
                futuros.add(pool.submit(acao));
            }
            List<T> resultados = new ArrayList<>();
            for (Future<T> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        }
    }
}
