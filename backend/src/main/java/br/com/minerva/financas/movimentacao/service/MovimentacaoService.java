package br.com.minerva.financas.movimentacao.service;

import br.com.minerva.financas.ativo.dao.IAtivoDAO;
import br.com.minerva.financas.ativo.dominio.Ativo;
import br.com.minerva.financas.comum.dominio.CalendarioNegociacao;
import br.com.minerva.financas.comum.dominio.ErroAplicacao;
import br.com.minerva.financas.contacorrente.dao.IContaCorrenteDAO;
import br.com.minerva.financas.movimentacao.dao.IMovimentacaoDAO;
import br.com.minerva.financas.movimentacao.dominio.Movimentacao;
import br.com.minerva.financas.movimentacao.dominio.TipoMovimentacaoEnum;
import br.com.minerva.financas.usuario.dao.IUsuarioDAO;
import br.com.minerva.financas.usuario.dominio.IProprietarioAtual;
import br.com.minerva.financas.usuario.helper.Capacidades;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Regra de compra e venda e o lançamento espelho, na mesma transação. */
@Service
public class MovimentacaoService {

    private final IMovimentacaoDAO movimentacoes;
    private final IContaCorrenteDAO conta;
    private final IAtivoDAO ativos;
    private final IUsuarioDAO usuarios;
    private final IProprietarioAtual proprietario;

    public MovimentacaoService(IMovimentacaoDAO movimentacoes, IContaCorrenteDAO conta,
                               IAtivoDAO ativos, IUsuarioDAO usuarios, IProprietarioAtual proprietario) {
        this.movimentacoes = movimentacoes;
        this.conta = conta;
        this.ativos = ativos;
        this.usuarios = usuarios;
        this.proprietario = proprietario;
    }

    @Transactional
    public void comprar(String codigo, LocalDate data, long quantidadeE2, long valorCentavos) {
        executar(codigo, data, quantidadeE2, valorCentavos, TipoMovimentacaoEnum.COMPRA);
    }

    @Transactional
    public void vender(String codigo, LocalDate data, long quantidadeE2, long valorCentavos) {
        executar(codigo, data, quantidadeE2, valorCentavos, TipoMovimentacaoEnum.VENDA);
    }

    public List<Movimentacao> listar(LocalDate inicio, LocalDate fim) {
        Capacidades.exigirUsuarioComum(proprietario);
        return movimentacoes.listar(usuario(), inicio, fim);
    }

    private void executar(String codigo, LocalDate data, long quantidadeE2, long valorCentavos,
                          TipoMovimentacaoEnum tipo) {
        Capacidades.exigirUsuarioComum(proprietario);
        Ativo ativo = ativos.ativo(codigo).orElseThrow(MovimentacaoService::ativoNaoEncontrado);
        if (!CalendarioNegociacao.ehDiaUtil(data)) {
            throw new ErroAplicacao("DATA_NAO_UTIL", 400,
                    "Movimentações só são permitidas de segunda a sexta-feira.");
        }
        if (!ativo.dentroDaJanela(data)) {
            throw new ErroAplicacao("FORA_DA_JANELA", 400,
                    "A data está fora da janela de negociação do ativo, que vai da emissão, inclusive, "
                            + "até o vencimento, exclusive.");
        }
        long usuario = usuario();
        if (tipo == TipoMovimentacaoEnum.COMPRA && !conta.saldoFuturoValido(usuario, data, -valorCentavos)) {
            throw new ErroAplicacao("SALDO_INSUFICIENTE", 409,
                    "O saldo ficaria negativo em alguma data a partir da data informada.");
        }
        if (tipo == TipoMovimentacaoEnum.VENDA
                && !movimentacoes.quantidadeFuturaValida(usuario, codigo, data, -quantidadeE2)) {
            throw new ErroAplicacao("QUANTIDADE_INSUFICIENTE", 409,
                    "A quantidade do ativo ficaria negativa em alguma data a partir da data informada.");
        }
        movimentacoes.inserir(usuario, codigo, data, tipo, quantidadeE2, valorCentavos);
        long deltaSaldo = tipo == TipoMovimentacaoEnum.COMPRA ? -valorCentavos : valorCentavos;
        conta.inserirLancamento(usuario, data, deltaSaldo,
                (tipo == TipoMovimentacaoEnum.COMPRA ? "Compra " : "Venda ") + ativo.codigo());
    }

    private long usuario() {
        return usuarios.idDoUsuario(proprietario.login());
    }

    private static ErroAplicacao ativoNaoEncontrado() {
        return new ErroAplicacao("ATIVO_NAO_ENCONTRADO", 404, "Ativo não encontrado.");
    }
}
