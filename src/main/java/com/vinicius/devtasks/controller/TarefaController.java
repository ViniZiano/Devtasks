package com.vinicius.devtasks.controller;

import com.vinicius.devtasks.dto.StatusDTO;
import com.vinicius.devtasks.dto.TarefaDTO;
import com.vinicius.devtasks.dto.TarefaRequestDTO;
import com.vinicius.devtasks.service.TarefaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/** Todas as rotas exigem token JWT; o Principal é o e-mail do usuário logado. */
@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    private final TarefaService service;

    public TarefaController(TarefaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TarefaDTO> criar(@RequestBody TarefaRequestDTO dto, Principal principal) {
        TarefaDTO criada = TarefaDTO.de(service.criar(principal.getName(), dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping
    public List<TarefaDTO> listar(Principal principal) {
        return service.listar(principal.getName()).stream()
                .map(TarefaDTO::de)
                .toList();
    }

    @GetMapping("/{id}")
    public TarefaDTO buscar(@PathVariable Long id, Principal principal) {
        return TarefaDTO.de(service.buscar(id, principal.getName()));
    }

    @PutMapping("/{id}")
    public TarefaDTO atualizar(@PathVariable Long id,
                               @RequestBody TarefaRequestDTO dto,
                               Principal principal) {
        return TarefaDTO.de(service.atualizar(id, principal.getName(), dto));
    }

    @PatchMapping("/{id}/status")
    public TarefaDTO atualizarStatus(@PathVariable Long id,
                                     @RequestBody StatusDTO dto,
                                     Principal principal) {
        return TarefaDTO.de(service.atualizarStatus(id, principal.getName(), dto.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, Principal principal) {
        service.deletar(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
