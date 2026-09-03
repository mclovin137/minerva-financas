package br.com.minerva.financas.contacorrente.rest;

import br.com.minerva.financas.comum.dominio.ErroAplicacao;
import br.com.minerva.financas.comum.helper.Requisicoes;
import br.com.minerva.financas.contacorrente.actor.ConsultarSaldoActor;
import br.com.minerva.financas.contacorrente.actor.LancamentoActorFactory;
import br.com.minerva.financas.contacorrente.actor.ListarLancamentosActor;
import br.com.minerva.financas.contacorrente.dominio.TipoLancamento;
import br.com.minerva.financas.contacorrente.dto.LancamentoRequisicao;
import br.com.minerva.financas.contacorrente.dto.LancamentoResposta;
import br.com.minerva.financas.contacorrente.dto.SaldoResposta;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** Rotas de conta corrente. As três primeiras são contratos literais do enunciado e não mudam de nível. */
@RestController
@RequestMapping(path = "/contacorrente", produces = MediaType.APPLICATION_JSON_VALUE)
class ContaCorrenteController {

    private final LancamentoActorFactory fabrica;
    private final ConsultarSaldoActor consultarSaldo;
    private final ListarLancamentosActor listarLancamentos;

    ContaCorrenteController(LancamentoActorFactory fabrica, ConsultarSaldoActor consultarSaldo,
                            ListarLancamentosActor listarLancamentos) {
        this.fabrica = fabrica;
        this.consultarSaldo = consultarSaldo;
        this.listarLancamentos = listarLancamentos;
    }

    @PostMapping(path = "/credito", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    void credito(@RequestBody(required = false) LancamentoRequisicao requisicao) {
        lancar(TipoLancamento.CREDITO, requisicao);
    }

    @PostMapping(path = "/debito", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    void debito(@RequestBody(required = false) LancamentoRequisicao requisicao) {
        lancar(TipoLancamento.DEBITO, requisicao);
    }

    private void lancar(TipoLancamento tipo, LancamentoRequisicao requisicao) {
        LancamentoRequisicao r = corpoObrigatorio(requisicao);
        fabrica.criar(tipo).executar(
                Requisicoes.escalado(r.valor(), "valor", 2, true),
                Requisicoes.texto(r.descricao(), "descricao"),
                Requisicoes.dataObrigatoria(r.data(), "data"));
    }

    @GetMapping("/saldo")
    SaldoResposta saldo(@RequestParam(name = "data", required = false) String data) {
        long centavos = consultarSaldo.executar(Requisicoes.filtroDeData(data, "data"));
        return new SaldoResposta(BigDecimal.valueOf(centavos, 2));
    }

    @GetMapping("/lancamentos")
    List<LancamentoResposta> lancamentos(@RequestParam(name = "dataInicio", required = false) String dataInicio,
                                         @RequestParam(name = "dataFim", required = false) String dataFim) {
        Requisicoes.Intervalo intervalo = Requisicoes.intervalo(dataInicio, dataFim);
        return listarLancamentos.executar(intervalo.inicio(), intervalo.fim());
    }

    private static LancamentoRequisicao corpoObrigatorio(LancamentoRequisicao requisicao) {
        if (requisicao == null) {
            throw new ErroAplicacao("REQUISICAO_INVALIDA", 400, "O corpo da requisição é obrigatório.");
        }
        return requisicao;
    }
}
