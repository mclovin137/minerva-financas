package br.com.minerva.financas.contacorrente.service;

import br.com.minerva.financas.comum.dominio.ErroAplicacao;
import br.com.minerva.financas.contacorrente.dao.IContaCorrenteDAO;
import br.com.minerva.financas.contacorrente.dominio.Lancamento;
import br.com.minerva.financas.usuario.dao.IUsuarioDAO;
import br.com.minerva.financas.usuario.dominio.IProprietarioAtual;
import br.com.minerva.financas.usuario.helper.Capacidades;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Regra de negócio da conta corrente: crédito, débito e a invariante de saldo não negativo. */
@Service
public class ContaCorrenteService {

    private final IContaCorrenteDAO dao;
    private final IUsuarioDAO usuarios;
    private final IProprietarioAtual proprietario;

    public ContaCorrenteService(IContaCorrenteDAO dao, IUsuarioDAO usuarios, IProprietarioAtual proprietario) {
        this.dao = dao;
        this.usuarios = usuarios;
        this.proprietario = proprietario;
    }

    @Transactional
    public void credito(long centavos, String descricao, LocalDate data) {
        Capacidades.exigirUsuarioComum(proprietario);
        dao.inserirLancamento(usuario(), data, centavos, descricao);
    }

    @Transactional
    public void debito(long centavos, String descricao, LocalDate data) {
        Capacidades.exigirUsuarioComum(proprietario);
        long usuario = usuario();
        if (!dao.saldoFuturoValido(usuario, data, -centavos)) {
            throw new ErroAplicacao("SALDO_INSUFICIENTE", 409,
                    "O saldo ficaria negativo em alguma data a partir da data informada.");
        }
        dao.inserirLancamento(usuario, data, -centavos, descricao);
    }

    public long saldo(LocalDate data) {
        Capacidades.exigirUsuarioComum(proprietario);
        return dao.saldo(usuario(), data);
    }

    public List<Lancamento> lancamentos(LocalDate inicio, LocalDate fim) {
        Capacidades.exigirUsuarioComum(proprietario);
        return dao.lancamentos(usuario(), inicio, fim);
    }

    private long usuario() {
        return usuarios.idDoUsuario(proprietario.login());
    }
}
