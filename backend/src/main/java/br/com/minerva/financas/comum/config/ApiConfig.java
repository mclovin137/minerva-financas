package br.com.minerva.financas.comum.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

/**
 * Configuração da fronteira JSON.
 * <p>
 * Números fracionários são desserializados como {@code BigDecimal}, nunca como {@code double}
 * (ADR-001 e FDD-001, D-A2): {@code 105.53} em ponto flutuante binário não é 105,53, e a diferença
 * aparece no centavo depois de algumas somas.
 */
@Configuration
class ApiConfig {

    @Bean
    JsonMapperBuilderCustomizer desserializarDecimaisComoBigDecimal() {
        return builder -> builder.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }
}
