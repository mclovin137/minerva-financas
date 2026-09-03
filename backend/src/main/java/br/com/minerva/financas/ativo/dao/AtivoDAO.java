package br.com.minerva.financas.ativo.dao;

import br.com.minerva.financas.ativo.dominio.Ativo;
import br.com.minerva.financas.ativo.dominio.TipoAtivo;
import br.com.minerva.financas.comum.dominio.PrecoUnitario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Único acesso SQL às tabelas {@code ativo} e {@code valor_mercado}. */
@Repository
public class AtivoDAO implements IAtivoDAO {

    private static final String SELECAO_ATIVO =
            "SELECT codigo, nome, tipo, data_emissao, data_vencimento FROM ativo";

    private final JdbcTemplate db;
    private final RowMapper<Ativo> mapeadorDeAtivo = (rs, linha) -> lerAtivo(rs);

    public AtivoDAO(JdbcTemplate db) {
        this.db = db;
    }

    private static Ativo lerAtivo(ResultSet rs) throws SQLException {
        return new Ativo(
                rs.getString("codigo"),
                rs.getString("nome"),
                TipoAtivo.valueOf(rs.getString("tipo")),
                LocalDate.parse(rs.getString("data_emissao")),
                LocalDate.parse(rs.getString("data_vencimento")));
    }

    @Override
    public void inserirAtivo(Ativo ativo) {
        db.update("INSERT INTO ativo (codigo, nome, tipo, data_emissao, data_vencimento) VALUES (?, ?, ?, ?, ?)",
                ativo.codigo(), ativo.nome(), ativo.tipo().name(),
                ativo.dataEmissao().toString(), ativo.dataVencimento().toString());
    }

    @Override
    public void atualizarAtivo(String codigo, Ativo ativo) {
        db.update("UPDATE ativo SET nome = ?, tipo = ?, data_emissao = ?, data_vencimento = ? WHERE codigo = ?",
                ativo.nome(), ativo.tipo().name(),
                ativo.dataEmissao().toString(), ativo.dataVencimento().toString(), codigo);
    }

    @Override
    public void removerAtivo(String codigo) {
        db.update("DELETE FROM ativo WHERE codigo = ?", codigo);
    }

    @Override
    public List<Ativo> ativos() {
        return db.query(SELECAO_ATIVO + " ORDER BY codigo", mapeadorDeAtivo);
    }

    @Override
    public Optional<Ativo> ativo(String codigo) {
        return db.query(SELECAO_ATIVO + " WHERE codigo = ?", mapeadorDeAtivo, codigo).stream().findFirst();
    }

    @Override
    public boolean ativoTemMovimentacao(String codigo) {
        Integer total = db.queryForObject("""
                SELECT COUNT(*) FROM movimentacao m JOIN ativo a ON a.id = m.ativo_id WHERE a.codigo = ?
                """, Integer.class, codigo);
        return total != null && total > 0;
    }

    @Override
    public boolean definirPreco(String codigo, LocalDate data, long precoE8) {
        long ativoId = idDoAtivo(codigo);
        int substituidos = db.update(
                "UPDATE valor_mercado SET preco_mercado_e8 = ? WHERE ativo_id = ? AND data = ?",
                precoE8, ativoId, data.toString());
        if (substituidos > 0) {
            return false;
        }
        db.update("INSERT INTO valor_mercado (ativo_id, data, preco_mercado_e8) VALUES (?, ?, ?)",
                ativoId, data.toString(), precoE8);
        return true;
    }

    @Override
    public boolean removerPreco(String codigo, LocalDate data) {
        return db.update("""
                DELETE FROM valor_mercado
                 WHERE data = ? AND ativo_id = (SELECT id FROM ativo WHERE codigo = ?)
                """, data.toString(), codigo) > 0;
    }

    @Override
    public Optional<PrecoUnitario> precoVigente(String codigo, LocalDate data) {
        List<Long> precos = db.queryForList("""
                SELECT vm.preco_mercado_e8 FROM valor_mercado vm
                  JOIN ativo a ON a.id = vm.ativo_id
                 WHERE a.codigo = ? AND vm.data <= ?
                 ORDER BY vm.data DESC LIMIT 1
                """, Long.class, codigo, data.toString());
        return precos.stream().findFirst().map(PrecoUnitario::new);
    }

    private long idDoAtivo(String codigo) {
        Long id = db.queryForObject("SELECT id FROM ativo WHERE codigo = ?", Long.class, codigo);
        if (id == null) {
            throw new IllegalStateException("Ativo inexistente: " + codigo);
        }
        return id;
    }
}
