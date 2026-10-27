package com.vinicius.devtasks.controller;

import com.vinicius.devtasks.dto.UsuarioCadastroDTO;
import com.vinicius.devtasks.dto.UsuarioResponseDTO;
import com.vinicius.devtasks.model.Usuario;
import com.vinicius.devtasks.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioController controller;

    @Test
    @DisplayName("Criar usuário deve gravar a senha com hash e devolver 201 sem a senha")
    void criarDeveGravarSenhaComHash() {
        when(repository.findByEmail("vini@teste.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("hash-bcrypt");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        ResponseEntity<UsuarioResponseDTO> resposta = controller.criar(
                new UsuarioCadastroDTO("Vinicius", "vini@teste.com", "1234"));

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertEquals(1L, resposta.getBody().id());
        assertEquals("Vinicius", resposta.getBody().nome());
        assertEquals("vini@teste.com", resposta.getBody().email());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        assertEquals("hash-bcrypt", captor.getValue().getSenha());
    }

    @Test
    @DisplayName("Criar usuário com e-mail já cadastrado deve responder 409")
    void criarComEmailDuplicadoDeveRetornar409() {
        when(repository.findByEmail("vini@teste.com")).thenReturn(Optional.of(new Usuario()));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controller.criar(new UsuarioCadastroDTO("Vinicius", "vini@teste.com", "1234")));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Criar usuário sem senha deve responder 400")
    void criarSemSenhaDeveRetornar400() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controller.criar(new UsuarioCadastroDTO("Vinicius", "vini@teste.com", " ")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Criar usuário sem e-mail deve responder 400")
    void criarSemEmailDeveRetornar400() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controller.criar(new UsuarioCadastroDTO("Vinicius", null, "1234")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    @DisplayName("Listar usuários deve devolver DTOs (sem senha)")
    void listarDeveRetornarDtosSemSenha() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Vinicius");
        usuario.setEmail("vini@teste.com");
        usuario.setSenha("hash-bcrypt");
        when(repository.findAll()).thenReturn(List.of(usuario));

        List<UsuarioResponseDTO> resultado = controller.listar();

        assertEquals(1, resultado.size());
        assertEquals("vini@teste.com", resultado.get(0).email());
    }
}
