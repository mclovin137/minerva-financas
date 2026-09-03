package br.com.minerva.financas.ativo.rest;

import br.com.minerva.financas.ativo.actor.AtualizarAtivoActor;
import br.com.minerva.financas.ativo.actor.ConsultarAtivoActor;
import br.com.minerva.financas.ativo.actor.CriarAtivoActor;
import br.com.minerva.financas.ativo.actor.DefinirPrecoActor;
import br.com.minerva.financas.ativo.actor.ListarAtivosActor;
import br.com.minerva.financas.ativo.actor.RemoverAtivoActor;
import br.com.minerva.financas.ativo.actor.RemoverPrecoActor;
import br.com.minerva.financas.ativo.dto.AtivoRequisicaoDTO;
import br.com.minerva.financas.ativo.dto.AtivoRespostaDTO;
import br.com.minerva.financas.ativo.dto.PrecoRequisicaoDTO;
import br.com.minerva.financas.ativo.dto.PrecoRespostaDTO;
import br.com.minerva.financas.comum.helper.Requisicoes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(path = "/ativos", produces = MediaType.APPLICATION_JSON_VALUE)
class AtivoController {

    private final ListarAtivosActor listar;
    private final ConsultarAtivoActor consultar;
    private final CriarAtivoActor criar;
    private final AtualizarAtivoActor atualizar;
    private final RemoverAtivoActor remover;
    private final DefinirPrecoActor definirPreco;
    private final RemoverPrecoActor removerPreco;

    AtivoController(ListarAtivosActor listar, ConsultarAtivoActor consultar, CriarAtivoActor criar,
                    AtualizarAtivoActor atualizar, RemoverAtivoActor remover,
                    DefinirPrecoActor definirPreco, RemoverPrecoActor removerPreco) {
        this.listar = listar;
        this.consultar = consultar;
        this.criar = criar;
        this.atualizar = atualizar;
        this.remover = remover;
        this.definirPreco = definirPreco;
        this.removerPreco = removerPreco;
    }

    @GetMapping
    List<AtivoRespostaDTO> listar() {
        return listar.executar();
    }

    @GetMapping("/{codigo}")
    AtivoRespostaDTO consultar(@PathVariable String codigo) {
        return consultar.executar(codigo);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    void criar(@RequestBody(required = false) AtivoRequisicaoDTO requisicao) {
        criar.executar(requisicao);
    }

    @PutMapping(path = "/{codigo}", consumes = MediaType.APPLICATION_JSON_VALUE)
    AtivoRespostaDTO atualizar(@PathVariable String codigo, @RequestBody(required = false) AtivoRequisicaoDTO requisicao) {
        return atualizar.executar(codigo, requisicao);
    }

    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remover(@PathVariable String codigo) {
        remover.executar(codigo);
    }

    @PutMapping(path = "/{codigo}/precos/{data}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PrecoRespostaDTO> definirPreco(@PathVariable String codigo, @PathVariable String data,
                                                @RequestBody(required = false) PrecoRequisicaoDTO requisicao) {
        if (requisicao == null) {
            throw new br.com.minerva.financas.comum.dominio.ErroAplicacao(
                    "REQUISICAO_INVALIDA", 400, "O corpo da requisição é obrigatório.");
        }
        LocalDate dia = Requisicoes.filtroDeData(data, "data");
        long preco = Requisicoes.escalado(requisicao.precoMercado(), "precoMercado", 8, false);
        var resultado = definirPreco.executar(codigo, dia, preco);
        return ResponseEntity.status(resultado.criado() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(resultado.resposta());
    }

    @DeleteMapping("/{codigo}/precos/{data}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removerPreco(@PathVariable String codigo, @PathVariable String data) {
        removerPreco.executar(codigo, Requisicoes.filtroDeData(data, "data"));
    }
}
