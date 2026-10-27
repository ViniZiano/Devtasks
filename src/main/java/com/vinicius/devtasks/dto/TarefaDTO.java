package com.vinicius.devtasks.dto;

import com.vinicius.devtasks.model.Tarefa;

public record TarefaDTO(
        Long id,
        String titulo,
        String status,
        String nomeUsuario
) {

    /** Converte a entidade em DTO. Aceita tarefa sem usuário (nomeUsuario fica null). */
    public static TarefaDTO de(Tarefa tarefa) {
        String nome = tarefa.getUsuario() != null ? tarefa.getUsuario().getNome() : null;
        return new TarefaDTO(tarefa.getId(), tarefa.getTitulo(), tarefa.getStatus(), nome);
    }
}
