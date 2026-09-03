package br.com.minerva.financas.comum.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Configuração de persistência conforme decisão D-A7 do FDD-001: toda conexão abre transação em
 * {@code BEGIN IMMEDIATE} (nunca {@code DEFERRED}), com {@code journal_mode=WAL},
 * {@code busy_timeout=5000} e {@code synchronous=NORMAL}, tudo programático via
 * {@link SQLiteConfig}, nunca via {@code PRAGMA} solto em {@code schema.sql}.
 * <p>
 * O perfil {@code test} (D-A10) usa um arquivo temporário exclusivo por execução, com os mesmos
 * PRAGMAs de produção — nunca {@code mode=memory&cache=shared}, que não exercitaria o WAL real — e
 * apagado ao final da execução.
 */
@Configuration
public class PersistenciaConfig {

    private static final Logger log = LoggerFactory.getLogger(PersistenciaConfig.class);
    private Path arquivoTeste;

    @Bean
    @Profile("!test")
    public DataSource dataSourceProducao(@Value("${spring.datasource.url}") String url) {
        garantirDiretorioDoBanco(url);
        return construirDataSource(url);
    }

    /**
     * O SQLite não cria o diretório do arquivo: apontar para {@code data/minerva-financas.db} numa
     * árvore limpa falha com {@code SQLITE_CANTOPEN} e a aplicação não sobe. Como a execução
     * standalone a partir de um clone recém-feito é requisito, o diretório é criado aqui.
     */
    private void garantirDiretorioDoBanco(String url) {
        String prefixo = "jdbc:sqlite:";
        if (!url.startsWith(prefixo)) {
            return;
        }
        String caminho = url.substring(prefixo.length());
        if (caminho.isBlank() || caminho.startsWith(":") || caminho.startsWith("file:")) {
            return;
        }
        Path diretorio = Path.of(caminho).toAbsolutePath().getParent();
        if (diretorio == null) {
            return;
        }
        try {
            Files.createDirectories(diretorio);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível criar o diretório do banco em " + diretorio, e);
        }
    }

    @Bean
    @Profile("test")
    public DataSource dataSourceTeste() throws IOException {
        Path arquivo = Files.createTempFile("minerva-test-" + UUID.randomUUID(), ".db");
        Files.deleteIfExists(arquivo);
        arquivoTeste = arquivo;
        arquivo.toFile().deleteOnExit();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> apagarSilenciosamente(arquivo)));
        String url = "jdbc:sqlite:" + arquivo.toAbsolutePath();
        return construirDataSource(url);
    }

    @jakarta.annotation.PreDestroy
    void apagarAoEncerrarContexto() {
        if (arquivoTeste != null) apagarSilenciosamente(arquivoTeste);
    }

    private void apagarSilenciosamente(Path arquivo) {
        try {
            Files.deleteIfExists(arquivo);
            Files.deleteIfExists(Path.of(arquivo + "-wal"));
            Files.deleteIfExists(Path.of(arquivo + "-shm"));
        } catch (IOException e) {
            log.warn("Não foi possível remover o banco de teste temporário {}: {}", arquivo, e.getMessage());
        }
    }

    private DataSource construirDataSource(String url) {
        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setBusyTimeout(5000);
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        config.enforceForeignKeys(true);

        SQLiteDataSource dataSource = new SQLiteDataSource(config);
        dataSource.setUrl(url);
        return dataSource;
    }
}
