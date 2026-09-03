package br.com.minerva.financas.usuario.dominio;

/** Principal resolvido a partir do cabeçalho HTTP Basic. */
public record UsuarioAutenticado(String login, boolean administrador) {
}
