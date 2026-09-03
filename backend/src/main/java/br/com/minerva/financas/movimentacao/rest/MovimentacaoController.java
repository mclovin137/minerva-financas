package br.com.minerva.financas.movimentacao.rest;

import br.com.minerva.financas.comum.helper.Requisicoes;
import br.com.minerva.financas.movimentacao.actor.IMovimentacaoActor;
import br.com.minerva.financas.movimentacao.actor.ListarMovimentacoesActor;
import br.com.minerva.financas.movimentacao.actor.MovimentacaoActorFactory;
import br.com.minerva.financas.movimentacao.dto.MovimentacaoRequisicao;
import br.com.minerva.financas.movimentacao.dto.MovimentacaoResposta;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(path = "/movimentacao", produces = MediaType.APPLICATION_JSON_VALUE)
class MovimentacaoController {

    private final MovimentacaoActorFactory fabrica;
    private final ListarMovimentacoesActor listar;

    MovimentacaoController(MovimentacaoActorFactory fabrica, ListarMovimentacoesActor listar) {
        this.fabrica = fabrica;
        this.listar = listar;
    }

    @PostMapping(path = "/compra", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    void comprar(@RequestBody(required = false) MovimentacaoRequisicao requisicao) {
        executar(fabrica.compra(), requisicao);
    }

    @PostMapping(path = "/venda", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    void vender(@RequestBody(required = false) MovimentacaoRequisicao requisicao) {
        executar(fabrica.venda(), requisicao);
    }

    @GetMapping
    List<MovimentacaoResposta> listar(@RequestParam(name = "dataInicio", required = false) String inicio,
                                   @RequestParam(name = "dataFim", required = false) String fim) {
        var intervalo = Requisicoes.intervalo(inicio, fim);
        return listar.executar(intervalo.inicio(), intervalo.fim());
    }

    private static void executar(IMovimentacaoActor actor, MovimentacaoRequisicao requisicao) {
        if (requisicao == null) {
            throw new br.com.minerva.financas.comum.dominio.ErroAplicacao(
                    "REQUISICAO_INVALIDA", 400, "O corpo da requisição é obrigatório.");
        }
        actor.executar(Requisicoes.texto(requisicao.ativo(), "ativo"),
                Requisicoes.dataObrigatoria(requisicao.data(), "data"),
                Requisicoes.escalado(requisicao.quantidade(), "quantidade", 2, true),
                Requisicoes.escalado(requisicao.valor(), "valor", 2, true));
    }
}
