package br.com.minerva.financas.usuario.dominio;

/**
 * Quem é o dono dos dados financeiros da requisição em curso, e o que ele pode fazer.
 * <p>
 * Nos níveis 1 e 2 não havia autenticação e o proprietário era o usuário fixo {@code usuario0}
 * (FDD-001, D-A9). A opção A do nível 3 troca apenas esta resolução pelo principal autenticado via
 * HTTP Basic — sem migration e sem coluna anulável. Os dados criados antes da opção A permanecem
 * sob {@code usuario0}: custo aceito e registrado.
 */
public interface IProprietarioAtual {

    String USUARIO_PADRAO = "usuario0";

    String login();

    /**
     * O administrativo gerencia ativos e preços e <strong>não</strong> transaciona nem consulta dados
     * confidenciais; o usuário comum faz o oposto. As duas capacidades são disjuntas de propósito.
     */
    boolean administrador();
}
