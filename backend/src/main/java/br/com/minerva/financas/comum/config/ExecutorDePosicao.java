package br.com.minerva.financas.comum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pools dedicados da consulta de posição assíncrona (FDD-003, D-C5).
 * <p>
 * São <strong>dois</strong>, e não um. O coordenador de uma execução fica bloqueado esperando as
 * partições dela terminarem; se coordenador e partições disputassem o mesmo pool fixo, bastariam
 * execuções simultâneas suficientes para que todos os threads estivessem bloqueados esperando
 * tarefas que jamais seriam agendadas — um deadlock clássico de auto-submissão.
 * <p>
 * Ambos são separados do pool que atende o HTTP: uma consulta de 200.000 movimentações não pode
 * consumir os threads que respondem as demais rotas.
 */
@Configuration
public class ExecutorDePosicao {

    /** Mínimo de 2 para garantir paralelismo observável; teto de 4 para não disputar CPU com o HTTP. */
    public static final int PARTICOES = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));

    public static final String PREFIXO_DO_WORKER = "posicao-worker-";
    private static final String PREFIXO_DO_COORDENADOR = "posicao-coordenador-";

    /** Executa a agregação das partições. É aqui que o paralelismo do enunciado acontece. */
    @Bean(name = "particoesDePosicao", destroyMethod = "shutdown")
    public ExecutorService particoesDePosicao() {
        return Executors.newFixedThreadPool(PARTICOES, fabricaDeThreads(PREFIXO_DO_WORKER));
    }

    /**
     * Coordena cada execução: dispara as partições e junta os resultados. O tamanho acompanha o
     * limite de execuções pendentes por usuário, para que a fila não cresça sem limite.
     */
    @Bean(name = "coordenadorDePosicao", destroyMethod = "shutdown")
    public ExecutorService coordenadorDePosicao() {
        return Executors.newFixedThreadPool(8, fabricaDeThreads(PREFIXO_DO_COORDENADOR));
    }

    private static ThreadFactory fabricaDeThreads(String prefixo) {
        AtomicInteger sequencia = new AtomicInteger();
        return tarefa -> {
            Thread thread = new Thread(tarefa, prefixo + sequencia.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
