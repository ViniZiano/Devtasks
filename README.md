# 🚀 DevTasks API
 
API REST desenvolvida com **Java e Spring Boot** para gerenciamento de tarefas, com autenticação de usuários utilizando **JWT (JSON Web Token)**.
 
Este projeto foi criado com foco em práticas de desenvolvimento backend, incluindo construção de endpoints REST, integração com banco de dados e autenticação segura.
 
---
 
## 📌 Funcionalidades
 
- ✅ Cadastro de usuários
- ✅ Login com autenticação
- ✅ Geração de token JWT
- ✅ CRUD de tarefas
- ✅ Integração com banco de dados relacional (MySQL)
---
 
## 🛠️ Tecnologias e Ferramentas
 
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- JWT (JSON Web Token)
- Lombok
- Maven
- Postman
---
 
## 🔐 Autenticação com JWT
 
A autenticação da API é baseada em **token JWT**, retornado após login válido.
 
### 📍 Endpoint
 
```
POST /auth/login
```
 
### 📥 Requisição
 
```json
{
  "email": "teste@teste.com",
  "senha": "1234"
}
```
 
### 📤 Resposta (200 OK)
 
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
 
---
 
## 📂 Endpoints principais
 
 
| Método | Rota             | Descrição                  | Autenticação |
|--------|-------------------|-----------------------------|--------------|
| POST   | `/auth/register`  | Cadastra um novo usuário    | Não          |
| POST   | `/auth/login`      | Autentica e retorna o token | Não          |
| GET    | `/tasks`           | Lista as tarefas do usuário | Sim (JWT)    |
| POST   | `/tasks`           | Cria uma nova tarefa        | Sim (JWT)    |
| PUT    | `/tasks/{id}`      | Atualiza uma tarefa         | Sim (JWT)    |
| DELETE | `/tasks/{id}`      | Remove uma tarefa           | Sim (JWT)    |
 
---
 
## 📸 Testes no Postman
 
![Teste do endpoint de login no Postman](docs/postman-login.png)
 
 
---
 
## 📂 Estrutura do Projeto
 
```
controller   → Endpoints REST
dto          → Entrada e saída de dados
model        → Entidades JPA
repository   → Acesso ao banco de dados
service      → Regras de negócio
security     → Autenticação e JWT
```
 
---
 
## ⚙️ Como executar o projeto
 
### 1. Clone o repositório
 
```
git clone https://github.com/ViniZiano/Devtasks.git
cd Devtasks
```
 
### 2. Configure o banco de dados
 
Crie um arquivo `application.properties` local (não versionado) com:
 
```
spring.datasource.url=jdbc:mysql://localhost:3306/devtasks
spring.datasource.username=root
spring.datasource.password=sua_senha
spring.jpa.hibernate.ddl-auto=update
```
 
 
### 3. Execute a aplicação
 
```
./mvnw spring-boot:run
```
 
A API sobe por padrão em `http://localhost:8080`.
 
---
 
## 🧪 Testes
 
Os testes dos endpoints foram realizados utilizando o **Postman**, validando:
 
- Autenticação de usuários
- Retorno de status HTTP corretos
- Geração de token JWT
> 🎯 **Próximo passo:** adicionar testes automatizados com JUnit 5 e Mockito para o `AuthService` e o `TaskService`, além de testes de integração para os endpoints principais.
 
---
 
## 🎨 Interface para portfólio
 
O projeto também conta com uma interface web responsiva em `frontend/`, criada para apresentar o DevTasks de forma visual e elegante.
 
### Como visualizar
 
1. Entre na pasta `frontend/`.
2. Abra o arquivo `index.html` no navegador.
3. Use **Nova tarefa** para adicionar tarefas e os filtros para alternar entre status.
 
### Destaques da interface
 
- Dashboard com indicadores de produtividade
- Lista de tarefas com filtros por status
- Criação de tarefas em modal
- Layout responsivo para desktop e celular
- Design moderno com tipografia, cards, badges e progresso visual
---
 
## 📈 Competências demonstradas
 
- Desenvolvimento de APIs REST
- Implementação de autenticação com JWT
- Escrita de queries com Spring Data JPA
- Integração com banco de dados relacional
- Organização em arquitetura em camadas
- Teste de APIs com Postman
---
 
## 🗺️ Roadmap
 
- [ ] Testes automatizados (JUnit 5 + Mockito)
- [ ] Documentação interativa com Swagger/OpenAPI
- [ ] Containerização com Docker e docker-compose
- [ ] Versionamento do banco com Flyway
- [ ] CI com GitHub Actions
- [ ] Integração real do frontend com a API
- [ ] Deploy (Render/Railway) com link público
---
 
## 👨‍💻 Autor
 
**Vinícius Figueiredo Ziano**
Estudante de Análise e Desenvolvimento de Sistemas (ADS) — UNIBRA
 
🔗 LinkedIn: [linkedin.com/in/ViniZiano](https://linkedin.com/in/ViniZiano)
🔗 GitHub: [github.com/ViniZiano](https://github.com/ViniZiano)
 
