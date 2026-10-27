package com.vinicius.devtasks.controller;

import com.vinicius.devtasks.model.Tarefa;
import com.vinicius.devtasks.model.Usuario;
import com.vinicius.devtasks.service.TarefaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TarefaControllerTest {

    @Mock
    private TarefaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standaloneSetup: sobe só o controller, sem carregar o Spring inteiro
        mockMvc = MockMvcBuilders.standaloneSetup(new TarefaController(service)).build();
    }

    private Tarefa criarTarefa(Long id, String titulo, String status, String nomeUsuario) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "nome", nomeUsuario);

        Tarefa tarefa = new Tarefa();
        ReflectionTestUtils.setField(tarefa, "id", id);
        tarefa.setTitulo(titulo);
        tarefa.setStatus(status);
        tarefa.setUsuario(usuario);
        return tarefa;
    }

    @Test
    @DisplayName("GET /tarefas deve devolver a lista no formato do TarefaDTO")
    void listarDeveRetornarDtos() throws Exception {
        when(service.listar()).thenReturn(List.of(
                criarTarefa(1L, "Estudar JUnit", "TODO", "Vinicius"),
                criarTarefa(2L, "Fazer deploy", "DONE", "Vinicius")));

        mockMvc.perform(get("/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Estudar JUnit"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].nomeUsuario").value("Vinicius"))
                .andExpect(jsonPath("$[1].status").value("DONE"));
    }

    @Test
    @DisplayName("GET /tarefas deve devolver lista vazia quando não há tarefas")
    void listarSemTarefasDeveRetornarListaVazia() throws Exception {
        when(service.listar()).thenReturn(List.of());

        mockMvc.perform(get("/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /tarefas/{id} deve devolver a tarefa encontrada")
    void buscarDeveRetornarTarefa() throws Exception {
        Tarefa tarefa = criarTarefa(7L, "Escrever README", "DOING", "Vinicius");
        when(service.buscarPorId(7L)).thenReturn(tarefa);

        mockMvc.perform(get("/tarefas/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.titulo").value("Escrever README"))
                .andExpect(jsonPath("$.status").value("DOING"))
                .andExpect(jsonPath("$.nomeUsuario").value("Vinicius"))
                // o DTO não expõe a entidade nem a lista de tarefas do usuário
                .andExpect(jsonPath("$.usuario").doesNotExist());
    }

    @Test
    @DisplayName("GET /tarefas/{id} de tarefa sem usuário deve devolver nomeUsuario nulo")
    void buscarTarefaSemUsuarioDeveRetornarNomeNulo() throws Exception {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Sem dono");
        tarefa.setStatus("TODO");
        when(service.buscarPorId(8L)).thenReturn(tarefa);

        mockMvc.perform(get("/tarefas/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Sem dono"))
                .andExpect(jsonPath("$.nomeUsuario").value(nullValue()));
    }

    @Test
    @DisplayName("POST /tarefas deve enviar os dados do JSON para o service")
    void criarDeveChamarServiceComDadosDoJson() throws Exception {
        when(service.salvar(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Nova tarefa", "descricao": "Detalhes", "status": "TODO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Nova tarefa"));

        ArgumentCaptor<Tarefa> captor = ArgumentCaptor.forClass(Tarefa.class);
        verify(service).salvar(captor.capture());
        assertEquals("Nova tarefa", captor.getValue().getTitulo());
        assertEquals("Detalhes", captor.getValue().getDescricao());
        assertEquals("TODO", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("DELETE /tarefas/{id} deve chamar o service e responder 200")
    void deletarDeveChamarService() throws Exception {
        mockMvc.perform(delete("/tarefas/3"))
                .andExpect(status().isOk());

        verify(service).deletar(3L);
    }
}
