package br.com.minerva.financas.ativo.actor;

import br.com.minerva.financas.ativo.service.AtivoService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class RemoverPrecoActor {

    private final AtivoService service;

    public RemoverPrecoActor(AtivoService service) {
        this.service = service;
    }

    public void executar(String codigo, LocalDate data) {
        service.removerPreco(codigo, data);
    }
}
