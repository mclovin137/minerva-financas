package br.com.minerva.financas.usuario.service;

import br.com.minerva.financas.usuario.dao.IUsuarioDAO;
import br.com.minerva.financas.usuario.dominio.UsuarioAutenticado;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Regra de autenticação da conta de usuário, consumida pelo adaptador HTTP. */
@Service
public class AutenticacaoService {
    private final IUsuarioDAO usuarios;

    public AutenticacaoService(IUsuarioDAO usuarios) {
        this.usuarios = usuarios;
    }

    public Optional<UsuarioAutenticado> autenticar(String login, String senha) {
        return usuarios.autenticar(login, senha);
    }
}
