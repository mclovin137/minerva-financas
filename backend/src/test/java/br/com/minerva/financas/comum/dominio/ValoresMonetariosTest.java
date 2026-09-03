package br.com.minerva.financas.comum.dominio;

import br.com.minerva.financas.comum.dominio.Dinheiro;
import br.com.minerva.financas.comum.dominio.PrecoUnitario;
import br.com.minerva.financas.comum.dominio.Quantidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Os três tipos de valor que carregam a precisão do sistema.
 * <p>
 * O enunciado separa duas escalas: reais não têm fração de centavo, e preços unitários podem ter
 * mais casas — como o preço da gasolina. Estes testes fixam essa separação, porque é onde um erro
 * silencioso de arredondamento entraria.
 */
class ValoresMonetariosTest {

    @Nested
    @DisplayName("Dinheiro: duas casas, guardado como centavos inteiros")
    class DinheiroTest {

        @Test
        void converteReaisParaCentavosSemPerda() {
            assertEquals(1242, Dinheiro.de(new BigDecimal("12.42")).centavos());
            assertEquals(700, Dinheiro.de(new BigDecimal("7")).centavos());
            assertEquals(1, Dinheiro.de(new BigDecimal("0.01")).centavos());
        }

        @ParameterizedTest(name = "{0} não é um valor monetário válido")
        @ValueSource(strings = {"12.421", "0.001", "-1.00", "-0.01"})
        void recusaEscalaExcedenteESinalNegativo(String valor) {
            assertThrows(IllegalArgumentException.class, () -> Dinheiro.de(new BigDecimal(valor)));
        }

        @Test
        void recusaValorForaDaFaixaDeLong() {
            BigDecimal absurdo = new BigDecimal("999999999999999999999.99");
            assertThrows(IllegalArgumentException.class, () -> Dinheiro.de(absurdo));
        }

        @Test
        void permiteValorNegativoQuandoConstruidoDeCentavos() {
            assertEquals(-3000, Dinheiro.deCentavos(-3000).centavos(),
                    "débito e lucro negativo são fatos legítimos; o que a fronteira recusa é entrada negativa");
        }
    }

    @Nested
    @DisplayName("Quantidade: duas casas, estritamente positiva")
    class QuantidadeTest {

        @Test
        void converteParaUnidadesDeCentesimo() {
            assertEquals(250, Quantidade.de(new BigDecimal("2.50")).unidadesE2());
            assertEquals(new BigDecimal("2.50"), Quantidade.de(new BigDecimal("2.5")).decimal());
        }

        @ParameterizedTest(name = "{0} não é uma quantidade válida")
        @ValueSource(strings = {"0", "0.00", "-1.00", "1.001"})
        void recusaZeroNegativoEEscalaExcedente(String valor) {
            assertThrows(IllegalArgumentException.class, () -> Quantidade.de(new BigDecimal(valor)));
        }
    }

    @Nested
    @DisplayName("PrecoUnitario: oito casas, como o preço da gasolina")
    class PrecoUnitarioTest {

        @Test
        void preservaAsOitoCasasDoEnunciado() {
            assertEquals(123456789L, PrecoUnitario.de(new BigDecimal("1.23456789")).unidadesE8());
            assertEquals(10553000000L, PrecoUnitario.de(new BigDecimal("105.53")).unidadesE8());
        }

        @Test
        void devolveSempreAEscalaOito() {
            assertEquals(new BigDecimal("105.53000000"), PrecoUnitario.de(new BigDecimal("105.53")).decimal());
        }

        @ParameterizedTest(name = "{0} não é um preço válido")
        @ValueSource(strings = {"1.234567891", "-0.00000001"})
        void recusaNonaCasaESinalNegativo(String valor) {
            assertThrows(IllegalArgumentException.class, () -> PrecoUnitario.de(new BigDecimal(valor)));
        }

        @Test
        void aceitaPrecoZero() {
            assertEquals(0, PrecoUnitario.de(BigDecimal.ZERO).unidadesE8(),
                    "preço zero é improvável, mas não é inválido; quantidade zero é que não existe");
        }
    }
}
