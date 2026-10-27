package com.vinicius.devtasks.dto;

/** Dados que o cliente envia para criar um usuário. */
public record UsuarioCadastroDTO(String nome, String email, String senha) {
}
