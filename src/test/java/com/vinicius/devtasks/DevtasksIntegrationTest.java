package com.vinicius.devtasks;

import com.vinicius.devtasks.model.Tarefa;
import com.vinicius.devtasks.model.Usuario;
import com.vinicius.devtasks.repository.TarefaRepository;
import com.vinicius.devtasks.repository.UsuarioRepository;
import com.vinicius.devtasks.security.JwtService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração: sobe o Spring completo com banco H2 em memória
 * (configurado em src/test/resources/application.properties) e exercita
 * os endpoints de ponta a ponta, passando pelo filtro de segurança JWT:
 * security -> controller -> service -> repository -> banco.
 */
@SpringBootTest
class DevtasksIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // adiciona a cadeia de filtros do Spring Security, como acontece no servidor de verdade
        Filter seguranca = context.getBean("springSecurityFilterChain", Filter.class);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(seguranca).build();
        tarefaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private String jsonUsuario(String nome, String email) {
        return """
                {"nome": "%s", "email": "%s", "senha": "1234"}
                """.formatted(nome, email);
    }

    private String jsonUsuario() {
        return jsonUsuario("Vinicius", "vini@teste.com");
    }

    private void cadastrarUsuario(String nome, String email) throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonUsuario(nome, email)))
                .andExpect(status().isCreated());
    }

    private void cadastrarUsuario() throws Exception {
        cadastrarUsuario("Vinicius", "vini@teste.com");
    }

    private String bearer(String email) {
        return "Bearer " + jwtService.gerarToken(email);
    }

    private long criarTarefa(String email, String titulo) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/tarefas")
                        .header("Authorization", bearer(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "%s", "descricao": "Detalhes", "status": "TODO"}
                                """.formatted(titulo)))
                .andExpect(status().isCreated())
                .andReturn();
        String corpo = resultado.getResponse().getContentAsString();
        return Long.parseLong(corpo.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    // ---------- cadastro e login (rotas públicas) ----------

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
    @DisplayName("Cadastro com e-mail repetido retorna 409 (e não 401, mesmo com a segurança ligada)")
    void cadastroComEmailRepetidoDeveRetornar409() throws Exception {
        cadastrarUsuario();

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonUsuario()))
                .andExpect(status().isConflict());
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

    // ---------- proteção das rotas ----------

    @Test
    @DisplayName("Rotas protegidas sem token respondem 401")
    void rotasProtegidasSemTokenDevemRetornar401() throws Exception {
        mockMvc.perform(get("/tarefas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/usuarios/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/tarefas/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token inválido ou adulterado responde 401")
    void tokenInvalidoDeveRetornar401() throws Exception {
        mockMvc.perform(get("/tarefas").header("Authorization", "Bearer isso.nao.vale"))
                .andExpect(status().isUnauthorized());

        String valido = jwtService.gerarToken("vini@teste.com");
        mockMvc.perform(get("/tarefas")
                        .header("Authorization", "Bearer " + valido.substring(0, valido.length() - 2) + "xx"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /usuarios/me devolve o usuário do token, sem senha")
    void meDeveRetornarUsuarioLogado() throws Exception {
        cadastrarUsuario();

        mockMvc.perform(get("/usuarios/me").header("Authorization", bearer("vini@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Vinicius"))
                .andExpect(jsonPath("$.email").value("vini@teste.com"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    // ---------- tarefas por usuário ----------

    @Test
    @DisplayName("Criar tarefa com token grava o dono do token e lista mostra o nome dele")
    void criarEListarTarefaDeveFuncionar() throws Exception {
        cadastrarUsuario();
        criarTarefa("vini@teste.com", "Estudar JUnit");

        mockMvc.perform(get("/tarefas").header("Authorization", bearer("vini@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Estudar JUnit"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].nomeUsuario").value("Vinicius"));
    }

    @Test
    @DisplayName("Cada usuário só enxerga e mexe nas próprias tarefas")
    void usuariosNaoDevemVerTarefasUmDoOutro() throws Exception {
        cadastrarUsuario("Vinicius", "vini@teste.com");
        cadastrarUsuario("Maria", "maria@teste.com");
        long idDoVini = criarTarefa("vini@teste.com", "Tarefa do Vini");

        // a Maria não vê a tarefa na listagem...
        mockMvc.perform(get("/tarefas").header("Authorization", bearer("maria@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // ...nem consegue ler, alterar ou apagar pelo id
        mockMvc.perform(get("/tarefas/" + idDoVini).header("Authorization", bearer("maria@teste.com")))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/tarefas/" + idDoVini + "/status")
                        .header("Authorization", bearer("maria@teste.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DONE\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/tarefas/" + idDoVini).header("Authorization", bearer("maria@teste.com")))
                .andExpect(status().isNotFound());

        // a tarefa do Vini continua intacta
        mockMvc.perform(get("/tarefas/" + idDoVini).header("Authorization", bearer("vini@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @DisplayName("Editar, mudar o status e apagar a própria tarefa")
    void editarMudarStatusEApagar() throws Exception {
        cadastrarUsuario();
        long id = criarTarefa("vini@teste.com", "Original");
        String auth = bearer("vini@teste.com");

        mockMvc.perform(put("/tarefas/" + id)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Editada\", \"descricao\": \"Nova\", \"status\": \"DOING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Editada"))
                .andExpect(jsonPath("$.status").value("DOING"));

        mockMvc.perform(patch("/tarefas/" + id + "/status")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(delete("/tarefas/" + id).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tarefas/" + id).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Criar tarefa sem título ou com status inválido retorna 400")
    void dadosInvalidosDevemRetornar400() throws Exception {
        cadastrarUsuario();
        String auth = bearer("vini@teste.com");

        mockMvc.perform(post("/tarefas")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"  \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/tarefas")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Ok\", \"status\": \"QUALQUER\"}"))
                .andExpect(status().isBadRequest());
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

        mockMvc.perform(get("/tarefas/" + tarefa.getId()).header("Authorization", bearer("vini@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Corrigir bug"))
                .andExpect(jsonPath("$.nomeUsuario").value("Vinicius"))
                .andExpect(jsonPath("$.usuario").doesNotExist());
    }

    // ---------- CORS (o site do GitHub Pages, em outra origem, chamando esta API) ----------

    private static final String SITE_PAGES = "https://viniziano.github.io";

    @Test
    @DisplayName("CORS: preflight vindo do site do GitHub Pages é aceito, sem exigir token")
    void preflightDoSiteDoPagesDeveSerAceito() throws Exception {
        mockMvc.perform(options("/tarefas")
                        .header("Origin", SITE_PAGES)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", SITE_PAGES))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    @DisplayName("CORS: origem desconhecida é barrada no preflight")
    void preflightDeOrigemDesconhecidaDeveSerRecusado() throws Exception {
        mockMvc.perform(options("/tarefas")
                        .header("Origin", "https://site-malicioso.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CORS: até a resposta 401 leva o cabeçalho, para o site conseguir ler o erro")
    void respostaDeErroTambemDeveTrazerCabecalhoCors() throws Exception {
        mockMvc.perform(get("/tarefas").header("Origin", SITE_PAGES))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", SITE_PAGES));
    }

    @Test
    @DisplayName("CORS: login feito pelo site do Pages devolve o token com o cabeçalho de origem")
    void loginPeloSiteDoPagesDeveFuncionar() throws Exception {
        cadastrarUsuario();

        mockMvc.perform(post("/auth/login")
                        .header("Origin", SITE_PAGES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"vini@teste.com\", \"senha\": \"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", SITE_PAGES));
    }
}
