package br.com.minerva.financas.ativo.service;

import br.com.minerva.financas.ativo.dao.IAtivoDAO;
import br.com.minerva.financas.ativo.dominio.Ativo;
import br.com.minerva.financas.comum.dominio.ErroAplicacao;
import br.com.minerva.financas.usuario.dominio.IProprietarioAtual;
import br.com.minerva.financas.usuario.helper.Capacidades;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Regra de negócio de ativos e do preço de mercado por data: acervo administrado, não transacional. */
@Service
public class AtivoService {

    private final IAtivoDAO dao;
    private final IProprietarioAtual proprietario;

    public AtivoService(IAtivoDAO dao, IProprietarioAtual proprietario) {
        this.dao = dao;
        this.proprietario = proprietario;
    }

    public List<Ativo> ativos() {
        return dao.ativos();
    }

    public Ativo ativo(String codigo) {
        return dao.ativo(codigo).orElseThrow(AtivoService::ativoNaoEncontrado);
    }

    /**
     * A checagem prévia é confiável porque a transação abre em {@code BEGIN IMMEDIATE} (D-A7): o
     * escritor já está serializado quando ela roda, então não há janela entre consultar e inserir.
     * O {@code catch} permanece como rede de segurança para o caso de a constraint ser violada por
     * um caminho que a checagem não cubra — nem todo driver traduz o erro de unicidade para a mesma
     * exceção, e um 500 aqui viraria falha do servidor para um conflito que é do cliente.
     */
    @Transactional
    public void criarAtivo(Ativo ativo) {
        Capacidades.exigirAdministrador(proprietario);
        if (dao.ativo(ativo.codigo()).isPresent()) {
            throw ativoDuplicado();
        }
        try {
            dao.inserirAtivo(ativo);
        } catch (DataAccessException e) {
            if (violaUnicidade(e)) {
                throw ativoDuplicado();
            }
            throw e;
        }
    }

    @Transactional
    public Ativo atualizarAtivo(String codigo, Ativo ativo) {
        Capacidades.exigirAdministrador(proprietario);
        exigirAtivo(codigo);
        dao.atualizarAtivo(codigo, ativo);
        return ativo;
    }

    @Transactional
    public void removerAtivo(String codigo) {
        Capacidades.exigirAdministrador(proprietario);
        exigirAtivo(codigo);
        if (dao.ativoTemMovimentacao(codigo)) {
            throw new ErroAplicacao("ATIVO_EM_USO", 409,
                    "O ativo possui movimentações e não pode ser removido.");
        }
        dao.removerAtivo(codigo);
    }

    @Transactional
    public boolean definirPreco(String codigo, LocalDate data, long precoE8) {
        Capacidades.exigirAdministrador(proprietario);
        exigirAtivo(codigo);
        return dao.definirPreco(codigo, data, precoE8);
    }

    @Transactional
    public void removerPreco(String codigo, LocalDate data) {
        Capacidades.exigirAdministrador(proprietario);
        exigirAtivo(codigo);
        if (!dao.removerPreco(codigo, data)) {
            throw new ErroAplicacao("RECURSO_NAO_ENCONTRADO", 404,
                    "Não há preço de mercado cadastrado para o ativo nessa data.");
        }
    }

    private Ativo exigirAtivo(String codigo) {
        return dao.ativo(codigo).orElseThrow(AtivoService::ativoNaoEncontrado);
    }

    private static ErroAplicacao ativoNaoEncontrado() {
        return new ErroAplicacao("ATIVO_NAO_ENCONTRADO", 404, "Ativo não encontrado.");
    }

    private static ErroAplicacao ativoDuplicado() {
        return new ErroAplicacao("ATIVO_DUPLICADO", 409, "Já existe um ativo com esse código.");
    }

    private static boolean violaUnicidade(DataAccessException e) {
        if (e instanceof DataIntegrityViolationException) {
            return true;
        }
        Throwable causa = e.getMostSpecificCause();
        String mensagem = causa == null ? e.getMessage() : causa.getMessage();
        return mensagem != null && mensagem.contains("UNIQUE constraint failed");
    }
}
