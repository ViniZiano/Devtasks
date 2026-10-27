package com.vinicius.devtasks.controller;

import com.vinicius.devtasks.dto.LoginDTO;
import com.vinicius.devtasks.model.Usuario;
import com.vinicius.devtasks.repository.UsuarioRepository;
import com.vinicius.devtasks.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController controller;

    private Usuario criarUsuario(String email, String senhaComHash) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenha(senhaComHash);
        return usuario;
    }

    @Test
    @DisplayName("Login com credenciais válidas deve retornar 200 e o token")
    void loginValidoDeveRetornarToken() {
        when(repository.findByEmail("vini@teste.com"))
                .thenReturn(Optional.of(criarUsuario("vini@teste.com", "hash-bcrypt")));
        when(passwordEncoder.matches("1234", "hash-bcrypt")).thenReturn(true);
        when(jwtService.gerarToken("vini@teste.com")).thenReturn("token-falso");

        ResponseEntity<String> resposta =
                controller.login(new LoginDTO("vini@teste.com", "1234"));

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("token-falso", resposta.getBody());
    }

    @Test
    @DisplayName("Login com e-mail inexistente deve retornar 401")
    void loginComEmailInexistenteDeveRetornar401() {
        when(repository.findByEmail("naoexiste@teste.com")).thenReturn(Optional.empty());

        ResponseEntity<String> resposta =
                controller.login(new LoginDTO("naoexiste@teste.com", "1234"));

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertEquals("Usuário não encontrado", resposta.getBody());
        verify(jwtService, never()).gerarToken(anyString());
    }

    @Test
    @DisplayName("Login com senha errada deve retornar 401 e não gerar token")
    void loginComSenhaErradaDeveRetornar401() {
        when(repository.findByEmail("vini@teste.com"))
                .thenReturn(Optional.of(criarUsuario("vini@teste.com", "hash-bcrypt")));
        when(passwordEncoder.matches("senha-errada", "hash-bcrypt")).thenReturn(false);

        ResponseEntity<String> resposta =
                controller.login(new LoginDTO("vini@teste.com", "senha-errada"));

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertEquals("Senha inválida", resposta.getBody());
        verify(jwtService, never()).gerarToken(anyString());
    }

    @Test
    @DisplayName("Login sem senha no corpo deve retornar 401 sem estourar erro")
    void loginSemSenhaDeveRetornar401() {
        when(repository.findByEmail("vini@teste.com"))
                .thenReturn(Optional.of(criarUsuario("vini@teste.com", "hash-bcrypt")));

        ResponseEntity<String> resposta =
                controller.login(new LoginDTO("vini@teste.com", null));

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        verify(jwtService, never()).gerarToken(anyString());
    }
}
