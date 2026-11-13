package com.vinicius.devtasks.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                // API stateless com token no cabeçalho: não usa cookie de sessão, então CSRF não se aplica
                .csrf(csrf -> csrf.disable())
                // libera o site do GitHub Pages (outra origem) a chamar esta API; usa o bean corsConfigurationSource
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // login e cadastro são públicos
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                        // o Spring encaminha erros (400, 404, 409...) para /error; sem liberar vira 401
                        .requestMatchers("/error").permitAll()
                        // health check (usado por Docker/hospedagem) e o site estático
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.svg").permitAll()
                        // todo o resto exige token válido
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS: o navegador só deixa o site em https://viniziano.github.io ler as respostas desta API
     * se ela disser que aceita aquela origem. As origens aceitas vêm de CORS_ORIGINS (separadas por vírgula).
     * O token vai no cabeçalho Authorization (não em cookie), então não precisa de allowCredentials.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:https://viniziano.github.io}") String origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(normalizarOrigens(origens));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Tira espaços e a barra final ("https://site.github.io/" não bate com a origem "https://site.github.io"). */
    static List<String> normalizarOrigens(String origens) {
        return Arrays.stream(origens.split(","))
                .map(String::trim)
                .map(o -> o.replaceAll("/+$", ""))
                .filter(o -> !o.isEmpty())
                .toList();
    }

    /**
     * A autenticação é feita pelo AuthController + JWT, não por usuário/senha do Spring.
     * Declarar este bean evita que o Spring Boot crie um usuário padrão com senha gerada no log.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Autenticação feita por token JWT");
        };
    }
}
