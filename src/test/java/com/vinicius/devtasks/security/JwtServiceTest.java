package com.vinicius.devtasks.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!";

    private JwtService jwtService;
    private SecretKey chave;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SEGREDO);
        chave = Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8));
    }

    private Claims lerClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    @DisplayName("Deve gerar um token JWT com 3 partes (header.payload.assinatura)")
    void deveGerarTokenNoFormatoJwt() {
        String token = jwtService.gerarToken("vini@teste.com");

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("O subject do token deve ser o e-mail informado")
    void tokenDeveConterEmailComoSubject() {
        String token = jwtService.gerarToken("vini@teste.com");

        assertEquals("vini@teste.com", lerClaims(token).getSubject());
    }

    @Test
    @DisplayName("O token deve expirar em aproximadamente 1 hora")
    void tokenDeveExpirarEmUmaHora() {
        long antes = System.currentTimeMillis();
        String token = jwtService.gerarToken("vini@teste.com");

        Date expiracao = lerClaims(token).getExpiration();
        long umaHora = 60 * 60 * 1000L;

        // margem de 1 minuto para o tempo de execução do teste
        assertTrue(expiracao.getTime() >= antes + umaHora - 60_000);
        assertTrue(expiracao.getTime() <= System.currentTimeMillis() + umaHora + 60_000);
    }

    @Test
    @DisplayName("Token assinado com outra chave deve ser rejeitado")
    void tokenComOutraChaveDeveSerRejeitado() {
        String token = jwtService.gerarToken("vini@teste.com");
        SecretKey outraChave = Keys.hmacShaKeyFor(
                "outra-chave-totalmente-diferente-com-32-bytes!!".getBytes(StandardCharsets.UTF_8));

        assertThrows(SignatureException.class, () ->
                Jwts.parser().verifyWith(outraChave).build().parseSignedClaims(token));
    }

    @Test
    @DisplayName("Sem segredo configurado a aplicação deve se recusar a subir")
    void segredoVazioDeveLancarExcecao() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> new JwtService(""));

        assertTrue(erro.getMessage().contains("JWT_SECRET"));
    }

    @Test
    @DisplayName("Segredo com menos de 32 caracteres deve ser recusado")
    void segredoCurtoDeveLancarExcecao() {
        assertThrows(IllegalStateException.class, () -> new JwtService("curto"));
    }
}
