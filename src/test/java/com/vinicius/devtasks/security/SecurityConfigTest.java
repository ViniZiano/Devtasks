package com.vinicius.devtasks.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityConfigTest {

    @Test
    @DisplayName("Origens do CORS: tira espaços e a barra final, que impediriam a comparação com o cabeçalho Origin")
    void deveNormalizarOrigens() {
        List<String> origens = SecurityConfig.normalizarOrigens(
                " https://viniziano.github.io/ , http://localhost:5500 ,, ");

        assertEquals(List.of("https://viniziano.github.io", "http://localhost:5500"), origens);
    }
}
