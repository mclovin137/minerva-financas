package br.com.minerva.financas.comum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serve a interface web a partir do mesmo processo da API.
 * <p>
 * Um artefato só: o avaliador abre uma URL, sem CORS para configurar e sem segundo servidor para
 * subir. Os arquivos entram em {@code static/} durante o build do contêiner; quando não estão
 * presentes, a aplicação continua funcionando como API pura.
 */
@Configuration
class RotasDaInterface implements WebMvcConfigurer {

    @Bean
    WebMvcConfigurer encaminharRaizParaAInterface() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(
                    org.springframework.web.servlet.config.annotation.ViewControllerRegistry registro) {
                registro.addViewController("/").setViewName("forward:/index.html");
            }
        };
    }
}
