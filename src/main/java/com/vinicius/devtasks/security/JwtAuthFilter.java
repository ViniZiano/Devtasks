package com.vinicius.devtasks.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lê o cabeçalho "Authorization: Bearer <token>". Se o token for válido,
 * marca a requisição como autenticada com o e-mail do usuário.
 * Se não houver token (ou for inválido), segue sem autenticação e o
 * Spring Security responde 401 nas rotas protegidas.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");

        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            jwtService.extrairEmail(cabecalho.substring(PREFIXO.length()).trim())
                    .ifPresent(email -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(email, null, List.of())));
        }

        chain.doFilter(request, response);
    }
}
