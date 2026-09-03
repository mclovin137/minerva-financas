package br.com.minerva.financas.usuario.dao;

import br.com.minerva.financas.usuario.dominio.UsuarioAutenticado;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Único acesso SQL à tabela {@code usuario}.
 * <p>
 * As senhas são guardadas como hash BCrypt, nunca em claro (FDD-003, D-C2). Quando o login não
 * existe, ainda assim é feita uma verificação contra um hash descartável: sem isso, o tempo de
 * resposta diria ao atacante quais logins existem.
 */
@Repository
public class UsuarioDAO implements IUsuarioDAO {

    /** Hash de uma senha que ninguém usa, só para igualar o custo do caminho "login inexistente". */
    private static final String HASH_DE_REFERENCIA =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final JdbcTemplate db;
    private final PasswordEncoder codificador = new BCryptPasswordEncoder();

    public UsuarioDAO(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public long idDoUsuario(String login) {
        Long id = db.queryForObject("SELECT id FROM usuario WHERE login = ?", Long.class, login);
        if (id == null) {
            throw new IllegalStateException("Usuário não encontrado no seed: " + login);
        }
        return id;
    }

    @Override
    public String codificar(String senha) {
        return codificador.encode(senha);
    }

    @Override
    public Optional<UsuarioAutenticado> autenticar(String login, String senha) {
        List<Registro> encontrados = db.query(
                "SELECT login, senha_hash, administrador FROM usuario WHERE login = ?",
                (rs, linha) -> new Registro(rs.getString("login"), rs.getString("senha_hash"),
                        rs.getInt("administrador") == 1),
                login);

        if (encontrados.isEmpty() || encontrados.getFirst().senhaHash() == null) {
            codificador.matches(senha, HASH_DE_REFERENCIA);
            return Optional.empty();
        }

        Registro registro = encontrados.getFirst();
        if (!codificador.matches(senha, registro.senhaHash())) {
            return Optional.empty();
        }
        return Optional.of(new UsuarioAutenticado(registro.login(), registro.administrador()));
    }

    private record Registro(String login, String senhaHash, boolean administrador) {
    }
}
