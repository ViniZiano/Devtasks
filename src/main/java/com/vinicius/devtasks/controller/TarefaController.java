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
    public TarefaDTO criar(@RequestBody Tarefa tarefa) {
        return TarefaDTO.de(service.salvar(tarefa));
    }

    @GetMapping
    public List<TarefaDTO> listar() {
        return service.listar().stream()
                .map(TarefaDTO::de)
                .toList();
    }

    @GetMapping("/{id}")
    public TarefaDTO buscar(@PathVariable Long id) {
        return TarefaDTO.de(service.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}
