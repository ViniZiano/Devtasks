package com.vinicius.devtasks.dto;

/**
 * Dados que o cliente envia para criar ou editar uma tarefa.
 * O dono da tarefa NÃO vem aqui: é sempre o usuário do token.
 */
public record TarefaRequestDTO(String titulo, String descricao, String status) {
}
