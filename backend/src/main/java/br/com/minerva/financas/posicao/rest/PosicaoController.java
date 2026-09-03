package br.com.minerva.financas.posicao.rest;

import br.com.minerva.financas.comum.helper.Requisicoes;
import br.com.minerva.financas.posicao.actor.PosicaoActorFactory;
import br.com.minerva.financas.posicao.dto.ExecucaoResposta;
import br.com.minerva.financas.posicao.dto.PosicaoResposta;
import br.com.minerva.financas.posicao.service.IPosicaoAssincronaService;
import br.com.minerva.financas.comum.dominio.ErroAplicacao;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/posicao", produces = MediaType.APPLICATION_JSON_VALUE)
class PosicaoController {

    private final PosicaoActorFactory actors;

    PosicaoController(PosicaoActorFactory actors) {
        this.actors = actors;
    }

    @GetMapping
    ResponseEntity<ExecucaoResposta> solicitar(@RequestParam(name = "data", required = false) String data) {
        long id = actors.solicitacao().executar(Requisicoes.filtroDeData(data, "data"));
        return ResponseEntity.accepted().body(new ExecucaoResposta(id));
    }

    @GetMapping("/{id}")
    ResponseEntity<List<PosicaoResposta>> resultado(@PathVariable long id) {
        var estado = actors.consulta().executar(id);
        return switch (estado) {
            case IPosicaoAssincronaService.Estado.EmAndamento ignorado ->
                    ResponseEntity.status(HttpStatus.TOO_EARLY).build();
            case IPosicaoAssincronaService.Estado.Falha falha ->
                    throw new ErroAplicacao("ERRO_INTERNO", 500, falha.mensagem());
            case IPosicaoAssincronaService.Estado.Concluida concluida ->
                    ResponseEntity.ok(actors.consulta().respostas(concluida.posicoes()));
        };
    }
}
