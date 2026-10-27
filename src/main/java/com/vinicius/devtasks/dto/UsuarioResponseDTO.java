package com.vinicius.devtasks.dto;

import com.vinicius.devtasks.model.Usuario;

/** Dados do usuário que a API devolve. Nunca inclui a senha. */
public record UsuarioResponseDTO(Long id, String nome, String email) {

    public static UsuarioResponseDTO de(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
