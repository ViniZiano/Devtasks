package com.vinicius.devtasks.repository;

import com.vinicius.devtasks.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
}