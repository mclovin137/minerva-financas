package br.com.minerva.financas.comum.config;

import br.com.minerva.financas.usuario.dao.IUsuarioDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Dados pré-cadastrados que o enunciado exige no ambiente disponibilizado: ativos de
 * {@code ATIVO0} a {@code ATIVO127}, com valor de mercado para {@code 2020-01-02}, e os usuários
 * {@code usuario0}–{@code usuario9} mais o administrador {@code root}.
 *
 * <h2>Premissa declarada</h2>
 * O enunciado fixa apenas os <em>nomes</em> dos ativos e a <em>data</em> do valor de mercado. Nome
 * de exibição, tipo, janela de negociação e preço de cada ativo não são especificados em lugar
 * nenhum, então são derivados aqui por uma regra determinística e reproduzível, concentrada nas
 * constantes abaixo para que trocá-la seja uma edição de uma linha. Está registrada no README como
 * premissa, não como requisito do enunciado.
 *
 * <p>A carga é idempotente: reexecutar a aplicação sobre o mesmo banco não duplica nem altera nada.
 */
@Component
public class SeedInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedInicial.class);

    static final int TOTAL_DE_ATIVOS = 128;
    static final String DATA_DO_PRECO_INICIAL = "2020-01-02";
    static final String EMISSAO_PADRAO = "2020-01-01";
    static final String VENCIMENTO_PADRAO = "2030-01-01";
    private static final String[] TIPOS = {"RV", "RF", "FUNDO"};

    private static final int TOTAL_DE_USUARIOS = 10;
    private static final String ADMINISTRADOR = "root";
    private static final String SENHA_DO_ADMINISTRADOR = "spiderman";

    private final JdbcTemplate db;
    private final IUsuarioDAO usuarios;

    public SeedInicial(JdbcTemplate db, IUsuarioDAO usuarios) {
        this.db = db;
        this.usuarios = usuarios;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        semearUsuarios();
        semearAtivos();
    }

    /** Preço de {@code ATIVOi}, em centavos: {@code 10,00 + i × 0,37}, de R$ 10,00 a R$ 56,99. */
    static long precoEmCentavos(int indice) {
        return 1000L + 37L * indice;
    }

    static String tipoDoAtivo(int indice) {
        return TIPOS[indice % TIPOS.length];
    }

    private void semearUsuarios() {
        List<Object[]> novos = new ArrayList<>();
        for (int i = 0; i < TOTAL_DE_USUARIOS; i++) {
            novos.add(new Object[]{"usuario" + i, usuarios.codificar("senha" + i), 0});
        }
        novos.add(new Object[]{ADMINISTRADOR, usuarios.codificar(SENHA_DO_ADMINISTRADOR), 1});

        int inseridos = 0;
        for (Object[] usuario : novos) {
            inseridos += db.update(
                    "INSERT OR IGNORE INTO usuario (login, senha_hash, administrador) VALUES (?, ?, ?)",
                    usuario);
        }
        // O usuario0 pode ter sido criado sem hash pelo schema dos níveis 1 e 2; a senha entra aqui.
        db.update("UPDATE usuario SET senha_hash = ? WHERE login = ? AND senha_hash IS NULL",
                usuarios.codificar("senha0"), "usuario0");

        if (inseridos > 0) {
            log.info("Seed de usuários concluído: {} credenciais criadas.", inseridos);
        }
    }

    private void semearAtivos() {
        Integer existentes = db.queryForObject(
                "SELECT COUNT(*) FROM ativo WHERE codigo LIKE 'ATIVO%'", Integer.class);
        if (existentes != null && existentes >= TOTAL_DE_ATIVOS) {
            return;
        }

        for (int i = 0; i < TOTAL_DE_ATIVOS; i++) {
            String codigo = "ATIVO" + i;
            db.update("""
                    INSERT OR IGNORE INTO ativo (codigo, nome, tipo, data_emissao, data_vencimento)
                    VALUES (?, ?, ?, ?, ?)
                    """, codigo, "Ativo " + i, tipoDoAtivo(i), EMISSAO_PADRAO, VENCIMENTO_PADRAO);

            db.update("""
                    INSERT OR IGNORE INTO valor_mercado (ativo_id, data, preco_mercado_e8)
                    SELECT id, ?, ? FROM ativo WHERE codigo = ?
                    """, DATA_DO_PRECO_INICIAL, precoEmCentavos(i) * 1_000_000L, codigo);
        }
        log.info("Seed de ativos concluído: ATIVO0 a ATIVO{} com preço em {}.",
                TOTAL_DE_ATIVOS - 1, DATA_DO_PRECO_INICIAL);
    }
}
