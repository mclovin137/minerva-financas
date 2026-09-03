package br.com.minerva.financas.usuario.helper;

import br.com.minerva.financas.usuario.dominio.UsuarioAutenticado;

/**
 * Principal da requisição em curso.
 * <p>
 * Usa {@link ThreadLocal} porque cada requisição é atendida por uma thread e nada aqui é
 * compartilhado entre elas — é justamente o oposto de guardar o usuário em um campo de bean
 * singleton, que é como uma aplicação concorrente passa a responder os dados de um usuário para
 * outro. O {@code finally} do filtro sempre limpa, para que a thread devolvida ao pool não carregue
 * o principal da requisição anterior.
 */
public final class ContextoDeSeguranca {

    private static final ThreadLocal<UsuarioAutenticado> ATUAL = new ThreadLocal<>();

    private ContextoDeSeguranca() {
    }

    public static void definir(UsuarioAutenticado usuario) {
        ATUAL.set(usuario);
    }

    public static UsuarioAutenticado atual() {
        UsuarioAutenticado usuario = ATUAL.get();
        if (usuario == null) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto da requisição.");
        }
        return usuario;
    }

    public static void limpar() {
        ATUAL.remove();
    }
}
