package br.com.minerva.financas.usuario.rest;

import br.com.minerva.financas.usuario.dominio.UsuarioAutenticado;
import br.com.minerva.financas.usuario.helper.ContextoDeSeguranca;
import br.com.minerva.financas.usuario.service.AutenticacaoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Autenticação HTTP Basic exigida em toda requisição (opção A do enunciado).
 * <p>
 * O 401 é respondido <strong>antes</strong> de qualquer verificação de existência de recurso: se a
 * rota respondesse 404 para um anônimo, o próprio código de status viraria um oráculo de
 * enumeração, dizendo a quem não se autenticou quais ativos e execuções existem (D-C1).
 * <p>
 * O cabeçalho {@code Authorization} nunca é registrado em log, nem em caso de falha.
 */
@Component
public class FiltroBasic extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroBasic.class);
    private static final String PREFIXO = "Basic ";

    private final AutenticacaoService autenticacao;

    public FiltroBasic(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    /**
     * A interface web é servida pelo mesmo host da API. Exigir Basic no HTML e no bundle faria o
     * navegador abrir sua própria caixa de diálogo antes de a aplicação carregar — a tela de entrada
     * desenhada nunca apareceria. Estes caminhos não expõem dado nenhum: são os arquivos estáticos
     * da própria interface.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        String caminho = requisicao.getRequestURI();
        return caminho.equals("/")
                || caminho.equals("/index.html")
                || caminho.equals("/favicon.ico")
                || caminho.startsWith("/assets/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        Optional<UsuarioAutenticado> usuario = autenticar(requisicao);
        if (usuario.isEmpty()) {
            log.debug("Requisição sem credencial válida em {} {}", requisicao.getMethod(),
                    requisicao.getRequestURI());
            recusar(resposta);
            return;
        }

        ContextoDeSeguranca.definir(usuario.get());
        try {
            cadeia.doFilter(requisicao, resposta);
        } finally {
            ContextoDeSeguranca.limpar();
        }
    }

    private Optional<UsuarioAutenticado> autenticar(HttpServletRequest requisicao) {
        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            return Optional.empty();
        }
        String decodificado;
        try {
            byte[] bytes = Base64.getDecoder().decode(cabecalho.substring(PREFIXO.length()).trim());
            decodificado = new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        int separador = decodificado.indexOf(':');
        if (separador < 0) {
            return Optional.empty();
        }
        return autenticacao.autenticar(decodificado.substring(0, separador), decodificado.substring(separador + 1));
    }

    private void recusar(HttpServletResponse resposta) throws IOException {
        resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resposta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"minerva\"");
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.getWriter().write("""
                {"codigo":"NAO_AUTENTICADO","mensagem":"Credenciais HTTP Basic ausentes ou inválidas."}""");
    }
}
