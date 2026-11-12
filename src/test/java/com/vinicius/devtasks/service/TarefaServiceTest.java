package com.vinicius.devtasks.service;

import com.vinicius.devtasks.dto.TarefaRequestDTO;
import com.vinicius.devtasks.model.Tarefa;
import com.vinicius.devtasks.model.Usuario;
import com.vinicius.devtasks.repository.TarefaRepository;
import com.vinicius.devtasks.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    private static final String EMAIL = "vini@teste.com";

    @Mock
    private TarefaRepository repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TarefaService service;

    private Tarefa tarefa(String titulo, String status) {
        Tarefa t = new Tarefa();
        t.setTitulo(titulo);
        t.setStatus(status);
        return t;
    }

    @Test
    @DisplayName("criar deve ligar a tarefa ao usuário do token e usar TODO como status padrão")
    void criarDeveLigarAoUsuarioEUsarStatusPadrao() {
        Usuario dono = new Usuario();
        dono.setEmail(EMAIL);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(dono));
        when(repository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

        Tarefa criada = service.criar(EMAIL, new TarefaRequestDTO("  Estudar JUnit ", "Detalhes", null));

        assertSame(dono, criada.getUsuario());
        assertEquals("Estudar JUnit", criada.getTitulo());
        assertEquals("Detalhes", criada.getDescricao());
        assertEquals("TODO", criada.getStatus());
    }

    @Test
    @DisplayName("criar sem título deve responder 400 e não salvar")
    void criarSemTituloDeveRetornar400() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new Usuario()));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(EMAIL, new TarefaRequestDTO("  ", null, "TODO")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criar com status inválido deve responder 400")
    void criarComStatusInvalidoDeveRetornar400() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new Usuario()));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(EMAIL, new TarefaRequestDTO("Tarefa", null, "QUALQUER")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    @DisplayName("criar com usuário inexistente (token antigo) deve responder 401")
    void criarComUsuarioInexistenteDeveRetornar401() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(EMAIL, new TarefaRequestDTO("Tarefa", null, "TODO")));

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    }

    @Test
    @DisplayName("listar deve buscar só as tarefas do e-mail informado")
    void listarDeveBuscarSoAsTarefasDoUsuario() {
        List<Tarefa> tarefas = List.of(tarefa("Tarefa 1", "TODO"), tarefa("Tarefa 2", "DONE"));
        when(repository.findByUsuarioEmailOrderByIdDesc(EMAIL)).thenReturn(tarefas);

        assertEquals(2, service.listar(EMAIL).size());
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("buscar deve devolver a tarefa quando ela é do usuário")
    void buscarDeveRetornarTarefaDoUsuario() {
        Tarefa t = tarefa("Minha", "TODO");
        when(repository.findByIdAndUsuarioEmail(1L, EMAIL)).thenReturn(Optional.of(t));

        assertSame(t, service.buscar(1L, EMAIL));
    }

    @Test
    @DisplayName("buscar tarefa que não existe (ou é de outro usuário) deve responder 404")
    void buscarTarefaDeOutroUsuarioDeveRetornar404() {
        when(repository.findByIdAndUsuarioEmail(99L, EMAIL)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.buscar(99L, EMAIL));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    @Test
    @DisplayName("atualizar deve trocar título, descrição e status")
    void atualizarDeveAlterarOsCampos() {
        Tarefa t = tarefa("Antigo", "TODO");
        when(repository.findByIdAndUsuarioEmail(1L, EMAIL)).thenReturn(Optional.of(t));
        when(repository.save(t)).thenReturn(t);

        Tarefa atualizada = service.atualizar(1L, EMAIL, new TarefaRequestDTO("Novo", "Desc", "doing"));

        assertEquals("Novo", atualizada.getTitulo());
        assertEquals("Desc", atualizada.getDescricao());
        assertEquals("DOING", atualizada.getStatus());
    }

    @Test
    @DisplayName("atualizarStatus deve mudar só o status")
    void atualizarStatusDeveMudarSoOStatus() {
        Tarefa t = tarefa("Tarefa", "TODO");
        when(repository.findByIdAndUsuarioEmail(1L, EMAIL)).thenReturn(Optional.of(t));
        when(repository.save(t)).thenReturn(t);

        Tarefa atualizada = service.atualizarStatus(1L, EMAIL, "DONE");

        assertEquals("Tarefa", atualizada.getTitulo());
        assertEquals("DONE", atualizada.getStatus());
    }

    @Test
    @DisplayName("deletar deve remover a tarefa do usuário")
    void deletarDeveRemoverTarefaDoUsuario() {
        Tarefa t = tarefa("Apagar", "TODO");
        when(repository.findByIdAndUsuarioEmail(5L, EMAIL)).thenReturn(Optional.of(t));

        service.deletar(5L, EMAIL);

        ArgumentCaptor<Tarefa> captor = ArgumentCaptor.forClass(Tarefa.class);
        verify(repository).delete(captor.capture());
        assertSame(t, captor.getValue());
    }

    @Test
    @DisplayName("deletar tarefa de outro usuário deve responder 404 e não apagar nada")
    void deletarTarefaDeOutroUsuarioDeveRetornar404() {
        when(repository.findByIdAndUsuarioEmail(5L, EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.deletar(5L, EMAIL));
        verify(repository, never()).delete(any());
    }
}
