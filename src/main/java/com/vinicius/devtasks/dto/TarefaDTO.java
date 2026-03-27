package com.vinicius.devtasks.dto;

public record TarefaDTO(
        Long id,
        String titulo,
        String status,
        String nomeUsuario
) {}