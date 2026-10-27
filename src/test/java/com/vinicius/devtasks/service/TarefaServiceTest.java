package com.vinicius.devtasks.service;

import com.vinicius.devtasks.model.Tarefa;
import com.vinicius.devtasks.repository.TarefaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    private TarefaRepository repository;

    @InjectMocks
    private TarefaService service;

    private Tarefa novaTarefa(String titulo, String status) {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setStatus(status);
        return tarefa;
    }

    @Test
    @DisplayName("salvar deve delegar ao repositório e devolver a tarefa salva")
    void salvarDeveRetornarTarefaSalva() {
        Tarefa entrada = novaTarefa("Estudar JUnit", "TODO");
        when(repository.save(entrada)).thenReturn(entrada);

        Tarefa resultado = service.salvar(entrada);

        assertSame(entrada, resultado);
        verify(repository).save(entrada);
    }

    @Test
    @DisplayName("listar deve devolver todas as tarefas do repositório")
    void listarDeveRetornarTodasAsTarefas() {
        List<Tarefa> tarefas = List.of(
                novaTarefa("Tarefa 1", "TODO"),
                novaTarefa("Tarefa 2", "DONE"));
        when(repository.findAll()).thenReturn(tarefas);

        List<Tarefa> resultado = service.listar();

        assertEquals(2, resultado.size());
        assertEquals("Tarefa 1", resultado.get(0).getTitulo());
        verify(repository).findAll();
    }

    @Test
    @DisplayName("listar deve devolver lista vazia quando não há tarefas")
    void listarDeveRetornarListaVazia() {
        when(repository.findAll()).thenReturn(List.of());

        assertTrue(service.listar().isEmpty());
    }

    @Test
    @DisplayName("buscarPorId deve devolver a tarefa quando ela existe")
    void buscarPorIdDeveRetornarTarefaExistente() {
        Tarefa tarefa = novaTarefa("Existente", "TODO");
        when(repository.findById(1L)).thenReturn(Optional.of(tarefa));

        assertSame(tarefa, service.buscarPorId(1L));
    }

    @Test
    @DisplayName("buscarPorId deve lançar exceção quando a tarefa não existe")
    void buscarPorIdDeveLancarExcecaoQuandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException erro = assertThrows(RuntimeException.class,
                () -> service.buscarPorId(99L));

        assertEquals("Tarefa não encontrada", erro.getMessage());
    }

    @Test
    @DisplayName("deletar deve chamar deleteById do repositório")
    void deletarDeveChamarRepositorio() {
        service.deletar(5L);

        verify(repository).deleteById(5L);
    }
}
