# ☁️ Como colocar o DevTasks no ar (passo a passo)

Este guia leva o projeto do seu computador até um **link público**. Informações de planos gratuitos conferidas em novembro de 2026; esses planos mudam com frequência, então confirme os limites nos sites oficiais antes de começar.

## Visão geral

O DevTasks precisa de 3 peças:

| Peça | O que é | Onde vai ficar |
|------|---------|----------------|
| Banco de dados | MySQL | TiDB Cloud Starter (grátis, compatível com MySQL) |
| API + site | Contêiner Docker (o mesmo `Dockerfile` do projeto) | Render (plano gratuito) |
| Configuração | Variáveis de ambiente (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`) | Painel do Render |
| Site no GitHub Pages | O mesmo site, ligado à API pelo arquivo `js/config.js` | `https://viniziano.github.io/Devtasks/` |

O site existe em dois lugares: o próprio Render já entrega o site junto com a API (mesmo endereço), e o GitHub Pages entrega uma cópia que conversa com a API do Render. O Passo 5 liga os dois.

> **Por que não o MySQL grátis da Aiven?** O plano gratuito de MySQL da Aiven está sendo encerrado: novos serviços gratuitos não podem mais ser criados e os existentes são desligados em 21/12/2026. O PlanetScale também não tem plano gratuito.

## Passo 0 — Teste tudo localmente

Antes de subir, confirme que funciona na sua máquina:

```
cp .env.example .env        # troque as senhas e o JWT_SECRET
docker compose up --build
```

Abra `http://localhost:8080`, crie uma conta e uma tarefa. Se funcionar aqui, o deploy é só trocar o endereço do banco.

## Passo 1 — Gerar o segredo do JWT

O `JWT_SECRET` assina os tokens. Quem o descobrir consegue forjar logins, então ele precisa ser longo, aleatório e **nunca** ir para o GitHub:

```
openssl rand -base64 48
```

Copie o resultado (mínimo de 32 caracteres; a aplicação se recusa a subir com menos).

## Passo 2 — Criar o banco MySQL (TiDB Cloud Starter)

1. Crie uma conta em [tidbcloud.com](https://tidbcloud.com). Não precisa de cartão para começar.
2. Crie uma instância **Starter** (gratuita). A cota gratuita informada na documentação é de 5 GiB de armazenamento e 50 milhões de Request Units por mês.
3. Abra o **SQL Editor** e crie o banco da aplicação:
   ```sql
   CREATE DATABASE devtasks;
   ```
4. Clique em **Connect** na instância e anote: **host**, **porta** (4000), **usuário** (costuma vir com um prefixo, como `abc123.root`) e **senha**.
5. Monte a `DB_URL` (o TiDB exige conexão criptografada):
   ```
   jdbc:mysql://SEU-HOST:4000/devtasks?sslMode=VERIFY_IDENTITY
   ```

> ⚠️ Eu não consegui testar o DevTasks contra o TiDB Cloud (a rede da minha máquina de desenvolvimento não alcança serviços externos). O TiDB é compatível com MySQL e o projeto usa o dialeto MySQL, mas teste localmente antes: rode `./mvnw spring-boot:run` com as variáveis abaixo apontando para o TiDB.

```
export DB_URL="jdbc:mysql://SEU-HOST:4000/devtasks?sslMode=VERIFY_IDENTITY"
export DB_USER="abc123.root"
export DB_PASSWORD="sua-senha"
export JWT_SECRET="o-segredo-do-passo-1"
./mvnw spring-boot:run
```

**Alternativa paga e simples:** o Railway (plano Hobby, cerca de US$ 5 por mês) hospeda a API e um MySQL no mesmo projeto, sem dormir. Confira o preço atual em [railway.com](https://railway.com).

## Passo 3 — Publicar a API e o site no Render

1. Crie uma conta em [render.com](https://render.com) entrando com o GitHub.
2. **New → Web Service** e escolha o repositório `ViniZiano/Devtasks`.
3. Configure:
   - **Language:** Docker (ele usa o `Dockerfile` da raiz)
   - **Branch:** `master`
   - **Instance Type:** Free
4. Em **Environment Variables**, adicione as quatro variáveis:

   | Nome | Valor |
   |------|-------|
   | `DB_URL` | a URL do passo 2 |
   | `DB_USER` | o usuário do passo 2 |
   | `DB_PASSWORD` | a senha do passo 2 |
   | `JWT_SECRET` | o segredo do passo 1 |

   Não é preciso definir `PORT`: o Render define sozinho e a aplicação lê essa variável.

   Opcional: `CORS_ORIGINS` diz quais sites podem chamar a API pelo navegador. O padrão já é `https://viniziano.github.io`, então só mexa nisso se usar outro endereço (separe vários por vírgula, sem barra no final).
5. Em **Health Check Path**, coloque `/actuator/health`.
6. Clique em **Create Web Service** e espere o build (alguns minutos na primeira vez).

Quando o status ficar **Live**, o Render mostra o link, algo como `https://devtasks-xxxx.onrender.com`.

## Passo 4 — Testar o link público

1. Abra o link, crie uma conta, entre e crie uma tarefa.
2. Teste a API:
   ```
   curl https://SEU-LINK.onrender.com/actuator/health
   ```
   Deve responder `{"status":"UP"}`.
3. Sem token, `GET /tarefas` deve responder 401.

## Passo 5 — Ligar o site do GitHub Pages à API

Hoje o site em `https://viniziano.github.io/Devtasks/` abre em **modo demonstração** (os dados ficam só no navegador). Para ele usar a API de verdade:

1. No repositório, abra o arquivo `js/config.js` **da raiz** (não o de `src/main/resources/static/`) e troque o endereço:
   ```js
   window.DEVTASKS_CONFIG = { apiUrl: 'https://devtasks-xxxx.onrender.com' };
   ```
   Use o link do Render, sem barra no final. O `js/config.js` de `src/main/resources/static/` continua com `apiUrl: ''` (no Render, site e API já estão no mesmo endereço).
2. Faça commit e `git push`. Em um ou dois minutos o GitHub Pages publica a nova versão.
3. Abra `https://viniziano.github.io/Devtasks/`, crie uma conta e uma tarefa. Confira no painel do banco (SQL Editor) que a tabela `tarefa` recebeu a linha.
4. Se o navegador mostrar um erro de **CORS** no Console (F12), confira se `CORS_ORIGINS` no Render está igual ao endereço do site (`https://viniziano.github.io`, sem barra no final) e se o serviço foi reiniciado depois de mudar a variável.

O site mostra o aviso _"Acordando o servidor…"_ quando o plano gratuito está dormindo e já começa a acordá-lo assim que a página abre. A demonstração continua disponível em `https://viniziano.github.io/Devtasks/?demo` para quem não quiser esperar.

## Passo 6 — Divulgar

- Coloque o link no topo do `README.md` (na linha **Demo online**).
- Coloque o link na descrição do repositório (⚙️ ao lado de _About_) e no card do projeto no portfólio.
- Cada `git push` na branch `master` publica uma nova versão automaticamente.

## O que esperar do plano gratuito

- O serviço **dorme após 15 minutos sem acessos** e leva cerca de 1 minuto para acordar no primeiro acesso. Avise isso no README e no portfólio (_"o primeiro acesso pode demorar cerca de 1 minuto"_), para quem avalia não achar que está quebrado.
- São 750 horas gratuitas por mês por conta; um único serviço ligado o mês todo cabe nisso.
- O Render pode reiniciar serviços gratuitos a qualquer momento. Como os dados ficam no banco externo, nada se perde.

## Segurança: o que nunca fazer

- Nunca coloque senha, `JWT_SECRET` ou o arquivo `.env` no GitHub (o `.env` já está no `.gitignore`).
- Se um segredo vazar, gere outro e troque no painel do Render; todos os tokens antigos deixam de valer.

## Checklist de um projeto "completo" para o currículo

| Item | Situação |
|------|----------|
| Testes automatizados | ✅ feito (JUnit 5 + Mockito + MockMvc) |
| Docker e Docker Compose | ✅ feito |
| CI com GitHub Actions | ✅ feito |
| README com endpoints e exemplos | ✅ feito |
| Autenticação JWT com rotas protegidas | ✅ feito |
| Site integrado à API | ✅ feito |
| Site do GitHub Pages com a API real (CORS) | ✅ pronto no código; falta o Passo 5 depois do deploy |
| Deploy com link público | 📄 este guia |
| Documentação interativa (Swagger/OpenAPI) | ⏳ ainda não implementado (roadmap) |
| Versionamento do banco (Flyway) | ⏳ ainda não implementado (roadmap) |
