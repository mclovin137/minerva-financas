package br.com.minerva.financas.posicao.service;

import br.com.minerva.financas.ativo.dao.IAtivoDAO;
import br.com.minerva.financas.ativo.dominio.Ativo;
import br.com.minerva.financas.comum.dominio.ErroAplicacao;
import br.com.minerva.financas.movimentacao.dao.ILeitorDeMovimentacoes;
import br.com.minerva.financas.posicao.dominio.CalculoPosicao;
import br.com.minerva.financas.posicao.dominio.Posicao;
import br.com.minerva.financas.usuario.dao.IUsuarioDAO;
import br.com.minerva.financas.usuario.dominio.IProprietarioAtual;
import br.com.minerva.financas.usuario.helper.Capacidades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Consulta assíncrona de posição com cursor incremental e partições independentes. */
@Service
public class PosicaoAssincronaService implements IPosicaoAssincronaService {

    private static final Logger log = LoggerFactory.getLogger(PosicaoAssincronaService.class);
    private static final Duration VALIDADE = Duration.ofMinutes(10);
    static final int RETIDAS_POR_USUARIO = 4;

    private final IAtivoDAO ativos;
    private final ILeitorDeMovimentacoes posicoes;
    private final IUsuarioDAO usuarios;
    private final IProprietarioAtual proprietario;
    private final ExecutorService coordenador;
    private final ExecutorService trabalhadores;
    private final int particoes;
    private final Set<String> threadsDaUltimaExecucao = ConcurrentHashMap.newKeySet();
    private final Map<Long, Execucao> execucoes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> retidasPorUsuario = new ConcurrentHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    public PosicaoAssincronaService(
            IAtivoDAO ativos, ILeitorDeMovimentacoes posicoes, IUsuarioDAO usuarios,
            IProprietarioAtual proprietario,
            @Qualifier("coordenadorDePosicao") ExecutorService coordenador,
            @Qualifier("particoesDePosicao") ExecutorService trabalhadores,
            @Value("${minerva.posicao.particoes:0}") int particoesConfiguradas) {
        this.ativos = ativos;
        this.posicoes = posicoes;
        this.usuarios = usuarios;
        this.proprietario = proprietario;
        this.coordenador = coordenador;
        this.trabalhadores = trabalhadores;
        this.particoes = particoesConfiguradas > 0 ? particoesConfiguradas
                : Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));
    }

    public Set<String> threadsDaUltimaExecucao() {
        return Set.copyOf(threadsDaUltimaExecucao);
    }

    @Override
    public long solicitar(LocalDate data) {
        Capacidades.exigirUsuarioComum(proprietario);
        String dono = proprietario.login();
        long usuario = usuarios.idDoUsuario(dono);
        AtomicInteger retidas = retidasPorUsuario.computeIfAbsent(dono, ignorado -> new AtomicInteger());
        if (retidas.incrementAndGet() > RETIDAS_POR_USUARIO) {
            retidas.decrementAndGet();
            throw new ErroAplicacao("EXCESSO_DE_EXECUCOES", 429,
                    "Há execuções demais retidas para este usuário. Consulte ou aguarde a expiração das anteriores.");
        }
        long id = proximoId.getAndIncrement();
        Execucao execucao = new Execucao(dono, Instant.now());
        try {
            execucao.tarefa = coordenador.submit(() -> calcular(usuario, data));
            execucoes.put(id, execucao);
            return id;
        } catch (RuntimeException e) {
            retidas.decrementAndGet();
            throw e;
        }
    }

    @Override
    public Optional<IPosicaoAssincronaService.Estado> consultar(long id) {
        Capacidades.exigirUsuarioComum(proprietario);
        Execucao execucao = execucoes.get(id);
        if (execucao == null || !execucao.dono.equals(proprietario.login())) {
            return Optional.empty();
        }
        synchronized (execucao) {
            if (!execucao.tarefa.isDone()) {
                return Optional.of(new IPosicaoAssincronaService.Estado.EmAndamento());
            }
            materializarResultado(execucao);
            if (execucao.falha != null) {
                remover(execucao, id);
                return Optional.of(new IPosicaoAssincronaService.Estado.Falha(execucao.falha));
            }
            if (!execucao.primeiraObservacaoConcluida) {
                execucao.primeiraObservacaoConcluida = true;
                return Optional.of(new IPosicaoAssincronaService.Estado.EmAndamento());
            }
            if (!execucoes.remove(id, execucao)) {
                return Optional.empty();
            }
            liberarReserva(execucao.dono);
            return Optional.of(new IPosicaoAssincronaService.Estado.Concluida(execucao.resultado));
        }
    }

    private void materializarResultado(Execucao execucao) {
        if (execucao.resultadoMaterializado) {
            return;
        }
        execucao.resultadoMaterializado = true;
        try {
            execucao.resultado = execucao.tarefa.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            execucao.falha = "A execução foi interrompida.";
        } catch (Exception e) {
            log.error("Execução de posição assíncrona falhou", e);
            execucao.falha = "A execução falhou ao processar a posição.";
        }
    }

    private void remover(Execucao execucao, long id) {
        if (execucoes.remove(id, execucao)) {
            liberarReserva(execucao.dono);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    void expirarExecucoesAbandonadas() {
        Instant limite = Instant.now().minus(VALIDADE);
        execucoes.entrySet().removeIf(entrada -> {
            if (entrada.getValue().criadaEm.isBefore(limite)) {
                liberarReserva(entrada.getValue().dono);
                return true;
            }
            return false;
        });
    }

    private void liberarReserva(String dono) {
        AtomicInteger retidas = retidasPorUsuario.get(dono);
        if (retidas != null) {
            retidas.decrementAndGet();
        }
    }

    private List<Posicao> calcular(long usuario, LocalDate data) {
        threadsDaUltimaExecucao.clear();
        Map<String, br.com.minerva.financas.comum.dominio.PrecoUnitario> precos = posicoes.precosVigentes(data);
        Map<String, Ativo> ativosPorCodigo = new HashMap<>();
        for (Ativo ativo : ativos.ativos()) {
            ativosPorCodigo.put(ativo.codigo(), ativo);
        }
        List<Future<Map<String, CalculoPosicao.Acumulador>>> parciais = new ArrayList<>();
        for (int particao = 0; particao < particoes; particao++) {
            int atual = particao;
            parciais.add(trabalhadores.submit(() -> agregarParticao(usuario, data, atual)));
        }
        List<Posicao> resultado = new ArrayList<>();
        for (Future<Map<String, CalculoPosicao.Acumulador>> parcial : parciais) {
            esperar(parcial).forEach((codigo, acumulador) -> {
                Ativo ativo = ativosPorCodigo.get(codigo);
                if (ativo != null) {
                    var preco = precos.get(codigo);
                    resultado.add(new Posicao(ativo, CalculoPosicao.calcular(acumulador, preco), preco));
                }
            });
        }
        resultado.sort(Comparator.comparing(posicao -> posicao.ativo().codigo()));
        log.info("Posição assíncrona concluída em {} partições, threads: {}", particoes,
                threadsDaUltimaExecucao);
        return resultado;
    }

    private Map<String, CalculoPosicao.Acumulador> agregarParticao(long usuario, LocalDate data, int particao) {
        threadsDaUltimaExecucao.add(Thread.currentThread().getName());
        Map<String, CalculoPosicao.Acumulador> agregado = new HashMap<>();
        posicoes.percorrer(usuario, data, particoes, particao, (codigo, tipo, quantidade, valor) ->
                agregado.computeIfAbsent(codigo, ignorado -> new CalculoPosicao.Acumulador())
                        .acumular(tipo, quantidade, valor));
        return agregado;
    }

    private static <T> T esperar(Future<T> futuro) {
        try {
            return futuro.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Agregação de posição interrompida.", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("Falha ao agregar uma partição da posição.", e.getCause());
        }
    }

    private static final class Execucao {
        private final String dono;
        private final Instant criadaEm;
        private Future<List<Posicao>> tarefa;
        private boolean resultadoMaterializado;
        private boolean primeiraObservacaoConcluida;
        private List<Posicao> resultado;
        private String falha;

        private Execucao(String dono, Instant criadaEm) {
            this.dono = dono;
            this.criadaEm = criadaEm;
        }
    }
}
