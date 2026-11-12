package com.vinicius.devtasks.controller;

import com.vinicius.devtasks.dto.TarefaRequestDTO;
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

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TarefaControllerTest {

    private static final String EMAIL = "vini@teste.com";
    private static final Principal LOGADO = () -> EMAIL;

    @Mock
    private TarefaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standaloneSetup: sobe só o controller; o usuário logado entra pelo .principal(...)
        mockMvc = MockMvcBuilders.standaloneSetup(new TarefaController(service)).build();
    }

    private Tarefa criarTarefa(Long id, String titulo, String status) {
        Usuario usuario = new Usuario();
        usuario.setNome("Vinicius");

        Tarefa tarefa = new Tarefa();
        ReflectionTestUtils.setField(tarefa, "id", id);
        tarefa.setTitulo(titulo);
        tarefa.setDescricao("Detalhes de " + titulo);
        tarefa.setStatus(status);
        tarefa.setUsuario(usuario);
        return tarefa;
    }

    @Test
    @DisplayName("GET /tarefas deve listar as tarefas do usuário logado no formato do DTO")
    void listarDeveRetornarDtosDoUsuarioLogado() throws Exception {
        when(service.listar(EMAIL)).thenReturn(List.of(
                criarTarefa(1L, "Estudar JUnit", "TODO"),
                criarTarefa(2L, "Fazer deploy", "DONE")));

        mockMvc.perform(get("/tarefas").principal(LOGADO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Estudar JUnit"))
                .andExpect(jsonPath("$[0].descricao").value("Detalhes de Estudar JUnit"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].nomeUsuario").value("Vinicius"))
                .andExpect(jsonPath("$[1].status").value("DONE"));
    }

    @Test
    @DisplayName("GET /tarefas/{id} deve devolver a tarefa sem expor a entidade do usuário")
    void buscarDeveRetornarTarefa() throws Exception {
        when(service.buscar(7L, EMAIL)).thenReturn(criarTarefa(7L, "Escrever README", "DOING"));

        mockMvc.perform(get("/tarefas/7").principal(LOGADO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.titulo").value("Escrever README"))
                .andExpect(jsonPath("$.usuario").doesNotExist());
    }

    @Test
    @DisplayName("POST /tarefas deve responder 201 e enviar ao service o e-mail do token + os dados do JSON")
    void criarDeveResponder201() throws Exception {
        when(service.criar(eq(EMAIL), any(TarefaRequestDTO.class)))
                .thenReturn(criarTarefa(10L, "Nova tarefa", "TODO"));

        mockMvc.perform(post("/tarefas")
                        .principal(LOGADO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Nova tarefa", "descricao": "Detalhes", "status": "TODO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.titulo").value("Nova tarefa"));

        ArgumentCaptor<TarefaRequestDTO> captor = ArgumentCaptor.forClass(TarefaRequestDTO.class);
        verify(service).criar(eq(EMAIL), captor.capture());
        assertEquals("Nova tarefa", captor.getValue().titulo());
        assertEquals("Detalhes", captor.getValue().descricao());
        assertEquals("TODO", captor.getValue().status());
    }

    @Test
    @DisplayName("PUT /tarefas/{id} deve atualizar a tarefa do usuário logado")
    void atualizarDeveChamarService() throws Exception {
        when(service.atualizar(eq(4L), eq(EMAIL), any(TarefaRequestDTO.class)))
                .thenReturn(criarTarefa(4L, "Editada", "DOING"));

        mockMvc.perform(put("/tarefas/4")
                        .principal(LOGADO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Editada", "descricao": "x", "status": "DOING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Editada"))
                .andExpect(jsonPath("$.status").value("DOING"));
    }

    @Test
    @DisplayName("PATCH /tarefas/{id}/status deve mudar só o status")
    void atualizarStatusDeveChamarService() throws Exception {
        when(service.atualizarStatus(4L, EMAIL, "DONE"))
                .thenReturn(criarTarefa(4L, "Tarefa", "DONE"));

        mockMvc.perform(patch("/tarefas/4/status")
                        .principal(LOGADO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @DisplayName("DELETE /tarefas/{id} deve chamar o service e responder 204")
    void deletarDeveResponder204() throws Exception {
        mockMvc.perform(delete("/tarefas/3").principal(LOGADO))
                .andExpect(status().isNoContent());

        verify(service).deletar(3L, EMAIL);
    }
}
