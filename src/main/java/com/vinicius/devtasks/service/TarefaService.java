package com.vinicius.devtasks.service;

import com.vinicius.devtasks.dto.TarefaRequestDTO;
import com.vinicius.devtasks.model.Tarefa;
import com.vinicius.devtasks.model.Usuario;
import com.vinicius.devtasks.repository.TarefaRepository;
import com.vinicius.devtasks.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * Regras das tarefas. Toda operação recebe o e-mail do usuário logado:
 * cada pessoa só enxerga e altera as próprias tarefas.
 */
@Service
public class TarefaService {

    static final Set<String> STATUS_VALIDOS = Set.of("TODO", "DOING", "DONE");

    private final TarefaRepository repository;
    private final UsuarioRepository usuarioRepository;

    public TarefaService(TarefaRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    public Tarefa criar(String email, TarefaRequestDTO dto) {
        Usuario dono = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

        Tarefa tarefa = new Tarefa();
        tarefa.setUsuario(dono);
        aplicar(tarefa, dto);
        return repository.save(tarefa);
    }

    public List<Tarefa> listar(String email) {
        return repository.findByUsuarioEmailOrderByIdDesc(email);
    }

    public Tarefa buscar(Long id, String email) {
        // tarefa de outro usuário responde 404 igual a tarefa que não existe (não revela que existe)
        return repository.findByIdAndUsuarioEmail(id, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));
    }

    public Tarefa atualizar(Long id, String email, TarefaRequestDTO dto) {
        Tarefa tarefa = buscar(id, email);
        aplicar(tarefa, dto);
        return repository.save(tarefa);
    }

    public Tarefa atualizarStatus(Long id, String email, String status) {
        Tarefa tarefa = buscar(id, email);
        tarefa.setStatus(normalizarStatus(status));
        return repository.save(tarefa);
    }

    public void deletar(Long id, String email) {
        repository.delete(buscar(id, email));
    }

    private void aplicar(Tarefa tarefa, TarefaRequestDTO dto) {
        if (dto == null || dto.titulo() == null || dto.titulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título é obrigatório");
        }
        tarefa.setTitulo(dto.titulo().trim());
        tarefa.setDescricao(dto.descricao() == null ? null : dto.descricao().trim());
        tarefa.setStatus(normalizarStatus(dto.status()));
    }

    /** Sem status vira TODO; status fora de TODO/DOING/DONE responde 400. */
    private String normalizarStatus(String status) {
        if (status == null || status.isBlank()) {
            return "TODO";
        }
        String normalizado = status.trim().toUpperCase();
        if (!STATUS_VALIDOS.contains(normalizado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status inválido. Use TODO, DOING ou DONE");
        }
        return normalizado;
    }
}
