package br.com.minerva.financas.ativo.actor;

import br.com.minerva.financas.ativo.builder.AtivoBuilder;
import br.com.minerva.financas.ativo.dto.PrecoRespostaDTO;
import br.com.minerva.financas.ativo.service.AtivoService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DefinirPrecoActor {

    private final AtivoService service;

    public DefinirPrecoActor(AtivoService service) {
        this.service = service;
    }

    /** @return o par (resposta, criado) — criado diferencia 201 de 200 no controller */
    public Resultado executar(String codigo, LocalDate data, long precoE8) {
        boolean criado = service.definirPreco(codigo, data, precoE8);
        return new Resultado(AtivoBuilder.respostaDePreco(codigo, data, precoE8), criado);
    }

    public record Resultado(PrecoRespostaDTO resposta, boolean criado) {
    }
}
