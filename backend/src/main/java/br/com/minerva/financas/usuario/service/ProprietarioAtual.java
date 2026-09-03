package br.com.minerva.financas.usuario.service;

import br.com.minerva.financas.usuario.dominio.IProprietarioAtual;
import br.com.minerva.financas.usuario.helper.ContextoDeSeguranca;
import org.springframework.stereotype.Component;

/** Liga o principal autenticado à porta que as demais entidades enxergam. */
@Component
public class ProprietarioAtual implements IProprietarioAtual {

    @Override
    public String login() {
        return ContextoDeSeguranca.atual().login();
    }

    @Override
    public boolean administrador() {
        return ContextoDeSeguranca.atual().administrador();
    }
}
