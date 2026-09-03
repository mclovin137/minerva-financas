package br.com.minerva.financas.comum.rest;

import br.com.minerva.financas.comum.dominio.ErroAplicacao;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Contrato de erro único dos três níveis (FDD-001, D-A6): todo 4xx e 5xx responde
 * {@code {codigo, mensagem, campos}}, em pt-BR, sem SQL, stack trace, caminho de arquivo ou
 * credencial.
 */
@RestControllerAdvice
class ErroApiHandler {

    private static final Logger log = LoggerFactory.getLogger(ErroApiHandler.class);

    record Campo(String campo, String mensagem) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record Erro(String codigo, String mensagem, List<Campo> campos) {
    }

    @ExceptionHandler(ErroAplicacao.class)
    ResponseEntity<Erro> negocio(ErroAplicacao e) {
        List<Campo> campos = e.campos.stream().map(c -> new Campo(c.campo(), c.mensagem())).toList();
        return resposta(HttpStatus.valueOf(e.status), e.codigo, e.getMessage(), campos);
    }

    /** JSON ausente, malformado ou com data/número que o parser não consegue converter. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Erro> corpoIlegivel(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA",
                "O corpo da requisição está ausente ou malformado.", List.of());
    }

    /**
     * Parâmetro de query ou de caminho que não converte para o tipo esperado. Sem este tratamento a
     * exceção cairia no catch-all e uma data inválida viraria 500 — erro do servidor para uma falha
     * que é do cliente.
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingRequestValueException.class})
    ResponseEntity<Erro> parametroInvalido(Exception e) {
        return resposta(HttpStatus.BAD_REQUEST, "FILTRO_INVALIDO",
                "Um parâmetro obrigatório está ausente ou em formato inválido.", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Erro> rotaInexistente(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "Recurso não encontrado.", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Erro> metodoNaoSuportado(HttpRequestMethodNotSupportedException e) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_PERMITIDO",
                "O método HTTP não é permitido nesta rota.", List.of());
    }

    /**
     * Rede de segurança. O detalhe fica no log do servidor, nunca na resposta: a mensagem devolvida
     * ao cliente não pode revelar SQL, caminho de arquivo nem estrutura interna (TC-062).
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Erro> interno(Exception e) {
        log.error("Falha não mapeada ao processar a requisição", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Ocorreu um erro interno ao processar a requisição.", List.of());
    }

    private static ResponseEntity<Erro> resposta(HttpStatus status, String codigo, String mensagem,
                                                 List<Campo> campos) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new Erro(codigo, mensagem, campos));
    }
}
