package com.vinicius.devtasks.repository;

import com.vinicius.devtasks.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    List<Tarefa> findByUsuarioEmailOrderByIdDesc(String email);

    Optional<Tarefa> findByIdAndUsuarioEmail(Long id, String email);
}
