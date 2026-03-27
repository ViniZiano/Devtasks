package com.vinicius.devtasks.controller;

import com.vinicius.devtasks.dto.TarefaDTO;
import com.vinicius.devtasks.model.Tarefa;
import com.vinicius.devtasks.service.TarefaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    private final TarefaService service;

    public TarefaController(TarefaService service) {
        this.service = service;
    }

    @PostMapping
    public Tarefa criar(@RequestBody Tarefa tarefa) {
        return service.salvar(tarefa);
    }

    @GetMapping
    public List<TarefaDTO> listar() {
        return service.listar().stream()
                .map(t -> new TarefaDTO(
                        t.getId(),
                        t.getTitulo(),
                        t.getStatus(),
                        t.getUsuario().getNome()
                ))
                .toList();
    }
    @GetMapping("/{id}")
    public Tarefa buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}