package br.com.minerva.financas.posicao.dao;

import br.com.minerva.financas.comum.dominio.Dinheiro;
import br.com.minerva.financas.comum.dominio.PrecoUnitario;
import br.com.minerva.financas.comum.dominio.Quantidade;
import br.com.minerva.financas.movimentacao.dao.ILeitorDeMovimentacoes;
import br.com.minerva.financas.movimentacao.dominio.Movimentacao;
import br.com.minerva.financas.movimentacao.dominio.TipoMovimentacaoEnum;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Único acesso SQL às leituras que compõem uma posição. */
@Repository
public class PosicaoDAO implements IPosicaoDAO, ILeitorDeMovimentacoes {

    private static final String SELECAO = """
            SELECT m.id, a.codigo, m.data, m.tipo, m.quantidade_e2, m.valor_centavos
              FROM movimentacao m JOIN ativo a ON a.id = m.ativo_id
            """;

    private final JdbcTemplate banco;
    private final RowMapper<Movimentacao> mapeador = (rs, linha) -> new Movimentacao(
            rs.getLong("id"), rs.getString("codigo"), LocalDate.parse(rs.getString("data")),
            TipoMovimentacaoEnum.valueOf(rs.getString("tipo")),
            new Quantidade(rs.getLong("quantidade_e2")),
            Dinheiro.deCentavos(rs.getLong("valor_centavos")));

    public PosicaoDAO(JdbcTemplate banco) {
        this.banco = banco;
    }

    @Override
    public List<Movimentacao> movimentacoesAte(long usuario, String codigo, LocalDate data) {
        return banco.query(SELECAO + " WHERE m.usuario_id = ? AND a.codigo = ? AND m.data <= ? ORDER BY m.data, m.id",
                mapeador, usuario, codigo, data.toString());
    }

    @Override
    public Optional<PrecoUnitario> precoVigente(String codigo, LocalDate data) {
        List<Long> precos = banco.queryForList("""
                SELECT vm.preco_mercado_e8 FROM valor_mercado vm
                  JOIN ativo a ON a.id = vm.ativo_id
                 WHERE a.codigo = ? AND vm.data <= ?
                 ORDER BY vm.data DESC LIMIT 1
                """, Long.class, codigo, data.toString());
        return precos.stream().findFirst().map(PrecoUnitario::new);
    }

    @Override
    public void percorrer(long usuario, LocalDate data, int totalDeParticoes, int particao,
                          ILeitorDeMovimentacoes.ConsumidorDeMovimento consumidor) {
        String sql = """
                SELECT a.codigo, m.tipo, m.quantidade_e2, m.valor_centavos
                  FROM movimentacao m JOIN ativo a ON a.id = m.ativo_id
                 WHERE m.usuario_id = ? AND m.data <= ? AND (m.ativo_id %% ?) = ?
                """.formatted();
        banco.query(conexao -> {
            var comando = conexao.prepareStatement(sql);
            comando.setFetchSize(1_000);
            comando.setLong(1, usuario);
            comando.setString(2, data.toString());
            comando.setInt(3, totalDeParticoes);
            comando.setInt(4, particao);
            return comando;
        }, (RowCallbackHandler) rs -> consumidor.aceitar(rs.getString(1),
                TipoMovimentacaoEnum.valueOf(rs.getString(2)), rs.getLong(3), rs.getLong(4)));
    }

    @Override
    public Map<String, PrecoUnitario> precosVigentes(LocalDate data) {
        Map<String, PrecoUnitario> precos = new HashMap<>();
        RowCallbackHandler acumulador = rs -> precos.put(rs.getString(1), new PrecoUnitario(rs.getLong(2)));
        banco.query("""
                SELECT a.codigo, vm.preco_mercado_e8
                  FROM ativo a JOIN valor_mercado vm ON vm.ativo_id = a.id
                 WHERE vm.data = (SELECT MAX(v2.data) FROM valor_mercado v2
                                   WHERE v2.ativo_id = a.id AND v2.data <= ?)
                """, acumulador, data.toString());
        return precos;
    }
}
