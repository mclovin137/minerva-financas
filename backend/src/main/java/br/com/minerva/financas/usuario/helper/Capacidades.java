package br.com.minerva.financas.usuario.helper;

import br.com.minerva.financas.comum.dominio.ErroAplicacao;
import br.com.minerva.financas.usuario.dominio.IProprietarioAtual;

/**
 * Autorização por capacidade, aplicada no caso de uso — não na serialização e não só na rota.
 * <p>
 * Fica junto de usuário porque "quem pode fazer o quê" deriva do papel do proprietário atual; as
 * demais entidades dependem desta classe, e não o contrário.
 */
public final class Capacidades {

    private Capacidades() {
    }

    /** Operações sobre o acervo compartilhado de ativos e preços: exclusivas do administrador. */
    public static void exigirAdministrador(IProprietarioAtual proprietario) {
        if (!proprietario.administrador()) {
            throw new ErroAplicacao("CAPACIDADE_NEGADA", 403,
                    "Somente um usuário administrativo pode gerenciar ativos e preços de mercado.");
        }
    }

    /** Operações sobre dados financeiros privados: proibidas ao administrador. */
    public static void exigirUsuarioComum(IProprietarioAtual proprietario) {
        if (proprietario.administrador()) {
            throw new ErroAplicacao("CAPACIDADE_NEGADA", 403,
                    "O usuário administrativo não gera lançamentos nem consulta dados de outros usuários.");
        }
    }
}
