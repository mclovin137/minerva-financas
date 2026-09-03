package br.com.minerva.financas.movimentacao.dao;

import br.com.minerva.financas.comum.dominio.Dinheiro;
import br.com.minerva.financas.comum.dominio.Quantidade;
import br.com.minerva.financas.movimentacao.dominio.Movimentacao;
import br.com.minerva.financas.movimentacao.dominio.TipoMovimentacao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** Único acesso SQL às escritas e à listagem de movimentações. */
@Repository
public class MovimentacaoDAO implements IMovimentacaoDAO {

    private static final String SELECAO = """
            SELECT m.id, a.codigo, m.data, m.tipo, m.quantidade_e2, m.valor_centavos
              FROM movimentacao m JOIN ativo a ON a.id = m.ativo_id
            """;

    private final JdbcTemplate banco;
    private final RowMapper<Movimentacao> mapeador = (rs, linha) -> new Movimentacao(
            rs.getLong("id"), rs.getString("codigo"), LocalDate.parse(rs.getString("data")),
            TipoMovimentacao.valueOf(rs.getString("tipo")),
            new Quantidade(rs.getLong("quantidade_e2")),
            Dinheiro.deCentavos(rs.getLong("valor_centavos")));

    public MovimentacaoDAO(JdbcTemplate banco) {
        this.banco = banco;
    }

    @Override
    public void inserir(long usuario, String codigo, LocalDate data, TipoMovimentacao tipo,
                        long quantidadeE2, long valorCentavos) {
        Long ativo = banco.queryForObject("SELECT id FROM ativo WHERE codigo = ?", Long.class, codigo);
        if (ativo == null) {
            throw new IllegalStateException("Ativo inexistente: " + codigo);
        }
        banco.update("""
                INSERT INTO movimentacao (usuario_id, ativo_id, data, tipo, quantidade_e2, valor_centavos)
                VALUES (?, ?, ?, ?, ?, ?)
                """, usuario, ativo, data.toString(), tipo.name(), quantidadeE2, valorCentavos);
    }

    @Override
    public boolean quantidadeFuturaValida(long usuario, String codigo, LocalDate data, long deltaE2) {
        String serie = """
                SELECT m.data,
                       SUM(SUM(CASE WHEN m.tipo = 'COMPRA' THEN m.quantidade_e2 ELSE -m.quantidade_e2 END))
                           OVER (ORDER BY m.data) AS acumulado
                  FROM movimentacao m JOIN ativo a ON a.id = m.ativo_id
                 WHERE m.usuario_id = ? AND a.codigo = ?
                 GROUP BY m.data
                """;
        Long minimoPosterior = banco.queryForObject(
                "SELECT MIN(acumulado) FROM (" + serie + ") WHERE data >= ?",
                Long.class, usuario, codigo, data.toString());
        Long quantidadeNaData = banco.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN m.tipo = 'COMPRA' THEN m.quantidade_e2 ELSE -m.quantidade_e2 END), 0)
                  FROM movimentacao m JOIN ativo a ON a.id = m.ativo_id
                 WHERE m.usuario_id = ? AND a.codigo = ? AND m.data <= ?
                """, Long.class, usuario, codigo, data.toString());
        long menor = quantidadeNaData == null ? 0L : quantidadeNaData;
        if (minimoPosterior != null) {
            menor = Math.min(menor, minimoPosterior);
        }
        return menor + deltaE2 >= 0;
    }

    @Override
    public List<Movimentacao> listar(long usuario, LocalDate inicio, LocalDate fim) {
        return banco.query(SELECAO + " WHERE m.usuario_id = ? AND m.data BETWEEN ? AND ? ORDER BY m.data, m.id",
                mapeador, usuario, inicio.toString(), fim.toString());
    }
}
