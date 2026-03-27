package com.vinicius.devtasks.controller;

import com.vinicius.devtasks.dto.LoginDTO;
import com.vinicius.devtasks.model.Usuario;
import com.vinicius.devtasks.repository.UsuarioRepository;
import com.vinicius.devtasks.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository repository;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDTO login) {

        var usuarioOpt = repository.findByEmail(login.email());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Usuário não encontrado");
        }

        Usuario usuario = usuarioOpt.get();

        if (!usuario.getSenha().equals(login.senha())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Senha inválida");
        }

        String token = jwtService.gerarToken(usuario.getEmail());

        return ResponseEntity.ok(token);
    }
}