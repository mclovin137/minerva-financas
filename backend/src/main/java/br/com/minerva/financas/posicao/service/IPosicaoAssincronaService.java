package br.com.minerva.financas.posicao.service;

import br.com.minerva.financas.posicao.dominio.Posicao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IPosicaoAssincronaService {
    long solicitar(LocalDate data);
    Optional<Estado> consultar(long id);

    sealed interface Estado {
        record EmAndamento() implements Estado {
        }
        record Concluida(List<Posicao> posicoes) implements Estado {
        }
        record Falha(String mensagem) implements Estado {
        }
    }
}
