package com.vinicius.devtasks.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final int TAMANHO_MINIMO_SEGREDO = 32;
    private static final long UMA_HORA_EM_MS = 1000L * 60 * 60;

    private final SecretKey key;

    /** O segredo vem da propriedade jwt.secret (variável de ambiente JWT_SECRET). */
    public JwtService(@Value("${jwt.secret:}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado: defina a variável de ambiente JWT_SECRET "
                            + "com pelo menos " + TAMANHO_MINIMO_SEGREDO + " caracteres.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(String email) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + UMA_HORA_EM_MS))
                .signWith(key)
                .compact();
    }

}
