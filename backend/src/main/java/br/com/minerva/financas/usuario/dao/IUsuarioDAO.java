package br.com.minerva.financas.usuario.dao;

import br.com.minerva.financas.usuario.dominio.UsuarioAutenticado;

import java.util.Optional;

/** Porta de acesso à tabela {@code usuario}: identificação e verificação de credencial. */
public interface IUsuarioDAO {

    long idDoUsuario(String login);

    String codificar(String senha);

    Optional<UsuarioAutenticado> autenticar(String login, String senha);
}
