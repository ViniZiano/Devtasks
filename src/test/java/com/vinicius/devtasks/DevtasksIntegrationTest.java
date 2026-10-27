package com.vinicius.devtasks;

import com.vinicius.devtasks.model.Tarefa;
import com.vinicius.devtasks.model.Usuario;
import com.vinicius.devtasks.repository.TarefaRepository;
import com.vinicius.devtasks.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração: sobe o Spring completo com banco H2 em memória
 * (configurado em src/test/resources/application.properties) e exercita
 * os endpoints de ponta a ponta: controller -> service -> repository -> banco.
 */
@SpringBootTest
class DevtasksIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        tarefaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private String jsonUsuario() {
        return """
                {"nome": "Vinicius", "email": "vini@teste.com", "senha": "1234"}
                """;
    }

    private void cadastrarUsuario() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonUsuario()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Cadastro não devolve a senha e grava apenas o hash no banco")
    void cadastroDeveGuardarHashENaoExporSenha() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonUsuario()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("vini@teste.com"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        Usuario salvo = usuarioRepository.findByEmail("vini@teste.com").orElseThrow();
        assertNotEquals("1234", salvo.getSenha());
        assertTrue(salvo.getSenha().startsWith("$2"), "a senha deveria estar em formato BCrypt");
    }

    @Test
    @DisplayName("Cadastro com e-mail repetido retorna 409")
    void cadastroComEmailRepetidoDeveRetornar409() throws Exception {
        cadastrarUsuario();

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonUsuario()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /usuarios não expõe a senha de ninguém")
    void listarUsuariosNaoDeveExporSenha() throws Exception {
        cadastrarUsuario();

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("vini@teste.com"))
                .andExpect(jsonPath("$[0].senha").doesNotExist());
    }

    @Test
    @DisplayName("Fluxo completo: cadastrar usuário e fazer login retorna um token JWT")
    void cadastrarELogarDeveRetornarToken() throws Exception {
        cadastrarUsuario();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "vini@teste.com", "senha": "1234"}
                                """))
                .andExpect(status().isOk())
                // formato de um JWT: três blocos base64url separados por ponto
                .andExpect(content().string(matchesPattern("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$")));
    }

    @Test
    @DisplayName("Login com senha errada retorna 401")
    void loginComSenhaErradaDeveRetornar401() throws Exception {
        cadastrarUsuario();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "vini@teste.com", "senha": "errada"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login com e-mail que não existe retorna 401")
    void loginComEmailInexistenteDeveRetornar401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ninguem@teste.com", "senha": "1234"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Criar uma tarefa para um usuário e listá-la mostra o nome do dono")
    void criarEListarTarefaDeveFuncionar() throws Exception {
        cadastrarUsuario();
        Usuario usuario = usuarioRepository.findByEmail("vini@teste.com").orElseThrow();

        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Estudar JUnit", "descricao": "Testes do DevTasks",
                                 "status": "TODO", "usuario": {"id": %d}}
                                """.formatted(usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Estudar JUnit"));

        mockMvc.perform(get("/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Estudar JUnit"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].nomeUsuario").value("Vinicius"));
    }

    @Test
    @DisplayName("GET /tarefas/{id} devolve o DTO sem entrar em loop com o usuário")
    void buscarTarefaPorIdNaoDeveEntrarEmLoop() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Vinicius");
        usuario.setEmail("vini@teste.com");
        usuario.setSenha("hash-qualquer");
        usuario = usuarioRepository.save(usuario);

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo("Corrigir bug");
        tarefa.setStatus("DOING");
        tarefa.setUsuario(usuario);
        tarefa = tarefaRepository.save(tarefa);

        mockMvc.perform(get("/tarefas/" + tarefa.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Corrigir bug"))
                .andExpect(jsonPath("$.nomeUsuario").value("Vinicius"))
                .andExpect(jsonPath("$.usuario").doesNotExist());
    }
}
