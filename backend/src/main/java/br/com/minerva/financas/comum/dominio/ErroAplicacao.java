package br.com.minerva.financas.comum.dominio;

import java.util.List;

/**
 * Erro de regra de negócio ou de validação, carregando diretamente o código do
 * catálogo do FDD-001 item 3, o status HTTP correspondente e, quando aplicável,
 * a lista de campos inválidos que compõe o contrato de erro único.
 */
public class ErroAplicacao extends RuntimeException {

    public record CampoErro(String campo, String mensagem) {
    }

    public final String codigo;
    public final int status;
    public final List<CampoErro> campos;

    public ErroAplicacao(String codigo, int status, String mensagem) {
        this(codigo, status, mensagem, List.of());
    }

    public ErroAplicacao(String codigo, int status, String mensagem, List<CampoErro> campos) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
        this.campos = campos;
    }

    public static ErroAplicacao campoInvalido(String campo, String mensagem) {
        return new ErroAplicacao("CAMPO_INVALIDO", 400, mensagem, List.of(new CampoErro(campo, mensagem)));
    }

    public static ErroAplicacao filtroInvalido(String mensagem) {
        return new ErroAplicacao("FILTRO_INVALIDO", 400, mensagem);
    }
}
