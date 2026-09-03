package br.com.minerva.financas.contacorrente.dao;

import br.com.minerva.financas.comum.dominio.Dinheiro;
import br.com.minerva.financas.contacorrente.dominio.Lancamento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Único acesso SQL à tabela {@code lancamento}.
 * <p>
 * As datas são gravadas como texto ISO {@code YYYY-MM-DD}, cuja ordenação lexicográfica coincide com
 * a cronológica — o que permite comparar e ordenar datas diretamente no SQL.
 */
@Repository
public class ContaCorrenteDAO implements IContaCorrenteDAO {

    private final JdbcTemplate db;

    public ContaCorrenteDAO(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public void inserirLancamento(long usuario, LocalDate data, long centavos, String descricao) {
        db.update("INSERT INTO lancamento (usuario_id, data, valor_centavos, descricao) VALUES (?, ?, ?, ?)",
                usuario, data.toString(), centavos, descricao);
    }

    @Override
    public long saldo(long usuario, LocalDate data) {
        Long saldo = db.queryForObject(
                "SELECT COALESCE(SUM(valor_centavos), 0) FROM lancamento WHERE usuario_id = ? AND data <= ?",
                Long.class, usuario, data.toString());
        return saldo == null ? 0L : saldo;
    }

    /**
     * O saldo nunca pode ficar negativo em <strong>nenhuma</strong> data, presente ou futura.
     * <p>
     * A série acumulada é agregada <strong>por data</strong>, e não linha a linha, porque o saldo do
     * enunciado é um conceito diário: um fato novo entra no fim do seu dia e não altera os estados
     * intermediários que já ocorreram dentro dele. Minimizar linha a linha rejeitaria uma compra
     * legítima só porque o saldo tocou zero entre dois lançamentos do mesmo dia.
     * <p>
     * O mínimo posterior é combinado com o saldo <em>na</em> data porque, quando não existe nenhum
     * lançamento em {@code data} ou depois, a subconsulta devolve {@code NULL} — e tratar isso como
     * zero rejeitaria um débito perfeitamente coberto pelo saldo acumulado antes.
     */
    @Override
    public boolean saldoFuturoValido(long usuario, LocalDate data, long delta) {
        Long minimoPosterior = db.queryForObject("""
                SELECT MIN(acumulado) FROM (
                    SELECT data, SUM(SUM(valor_centavos)) OVER (ORDER BY data) AS acumulado
                      FROM lancamento WHERE usuario_id = ?
                     GROUP BY data
                ) WHERE data >= ?
                """, Long.class, usuario, data.toString());

        long menorSaldo = saldo(usuario, data);
        if (minimoPosterior != null) {
            menorSaldo = Math.min(menorSaldo, minimoPosterior);
        }
        return menorSaldo + delta >= 0;
    }

    @Override
    public List<Lancamento> lancamentos(long usuario, LocalDate inicio, LocalDate fim) {
        return db.query("""
                SELECT id, data, valor_centavos, descricao FROM lancamento
                 WHERE usuario_id = ? AND data BETWEEN ? AND ?
                 ORDER BY data, id
                """, (rs, linha) -> new Lancamento(
                        rs.getLong("id"),
                        LocalDate.parse(rs.getString("data")),
                        Dinheiro.deCentavos(rs.getLong("valor_centavos")),
                        rs.getString("descricao")),
                usuario, inicio.toString(), fim.toString());
    }
}
