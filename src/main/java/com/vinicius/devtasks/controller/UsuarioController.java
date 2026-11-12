package com.vinicius.devtasks.controller;

import com.vinicius.devtasks.dto.UsuarioCadastroDTO;
import com.vinicius.devtasks.dto.UsuarioResponseDTO;
import com.vinicius.devtasks.model.Usuario;
import com.vinicius.devtasks.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@RequestBody UsuarioCadastroDTO dto) {
        if (dto.email() == null || dto.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail é obrigatório");
        }
        if (dto.senha() == null || dto.senha().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha é obrigatória");
        }
        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        Usuario salvo = repository.save(usuario);

        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponseDTO.de(salvo));
    }

    /** Dados de quem está logado (o e-mail vem do token JWT validado pelo filtro). */
    @GetMapping("/me")
    public UsuarioResponseDTO me(Principal principal) {
        Usuario usuario = repository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        return UsuarioResponseDTO.de(usuario);
    }
}
