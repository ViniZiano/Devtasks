/*
 * Modo demonstração do DevTasks.
 * O GitHub Pages só serve arquivos estáticos (não roda o Spring Boot), então, quando o site é aberto
 * sem a API, este arquivo imita as mesmas rotas e regras guardando tudo no localStorage do navegador.
 * Nada sai do navegador. Na versão real (Spring Boot + JWT + MySQL) este arquivo não é usado.
 */
(() => {
  'use strict';

  const KEY_USERS = 'devtasks.demo.users';
  const KEY_TASKS = 'devtasks.demo.tasks';
  const STATUS_VALIDOS = ['TODO', 'DOING', 'DONE'];

  class DemoError extends Error {
    constructor(status, message) { super(message); this.status = status; this.demo = true; }
  }

  /* ---------- armazenamento (com reserva em memória se o navegador bloquear o localStorage) ---------- */
  const memoria = {};
  function ler(chave, padrao) {
    try {
      const bruto = localStorage.getItem(chave);
      if (bruto !== null) return JSON.parse(bruto);
    } catch { /* usa a memória */ }
    return chave in memoria ? memoria[chave] : padrao;
  }
  function gravar(chave, valor) {
    memoria[chave] = valor;
    try { localStorage.setItem(chave, JSON.stringify(valor)); } catch { /* fica só em memória */ }
  }

  async function hashSenha(texto) {
    if (window.crypto && crypto.subtle) {
      const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(texto));
      return Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, '0')).join('');
    }
    return 'texto:' + texto;
  }

  /* ---------- regras iguais às da API ---------- */
  function normalizarStatus(status) {
    if (status === undefined || status === null || String(status).trim() === '') return 'TODO';
    const s = String(status).trim().toUpperCase();
    if (!STATUS_VALIDOS.includes(s)) throw new DemoError(400, 'Status inválido. Use TODO, DOING ou DONE');
    return s;
  }

  function aplicar(tarefa, dados) {
    if (!dados || typeof dados.titulo !== 'string' || !dados.titulo.trim()) {
      throw new DemoError(400, 'Título é obrigatório');
    }
    tarefa.titulo = dados.titulo.trim();
    tarefa.descricao = typeof dados.descricao === 'string' ? dados.descricao.trim() : null;
    tarefa.status = normalizarStatus(dados.status);
  }

  function emailDoToken(token) {
    if (typeof token !== 'string' || !token.startsWith('demo.')) return null;
    let email;
    try { email = decodeURIComponent(token.slice(5)); } catch { return null; }
    return ler(KEY_USERS, {})[email] ? email : null;
  }

  const dto = (t, nome) => ({ id: t.id, titulo: t.titulo, descricao: t.descricao, status: t.status, nomeUsuario: nome });

  /* ---------- rotas ---------- */
  async function handle(path, { method = 'GET', body, auth = true, token } = {}) {
    // públicas
    if (method === 'POST' && path === '/usuarios') return cadastrar(body || {});
    if (method === 'POST' && path === '/auth/login') return entrar(body || {});

    // protegidas: sem token válido, 401 (igual ao filtro JWT)
    const email = emailDoToken(token);
    if (!email) throw new DemoError(401, 'Não autenticado');
    const usuarios = ler(KEY_USERS, {});
    const nome = usuarios[email].nome;

    if (method === 'GET' && path === '/usuarios/me') return { id: 1, nome, email };

    const todas = ler(KEY_TASKS, []);
    const minhas = () => todas.filter((t) => t.dono === email);

    if (path === '/tarefas') {
      if (method === 'GET') return minhas().sort((a, b) => b.id - a.id).map((t) => dto(t, nome));
      if (method === 'POST') {
        const nova = { id: (todas.reduce((m, t) => Math.max(m, t.id), 0)) + 1, dono: email };
        aplicar(nova, body);
        todas.push(nova);
        gravar(KEY_TASKS, todas);
        return dto(nova, nome);
      }
    }

    const m = /^\/tarefas\/(\d+)(\/status)?$/.exec(path);
    if (m) {
      const id = Number(m[1]);
      const tarefa = minhas().find((t) => t.id === id);
      if (!tarefa) throw new DemoError(404, 'Tarefa não encontrada'); // de outro usuário também é 404
      if (method === 'PUT' && !m[2]) { aplicar(tarefa, body); gravar(KEY_TASKS, todas); return dto(tarefa, nome); }
      if (method === 'PATCH' && m[2]) { tarefa.status = normalizarStatus(body && body.status); gravar(KEY_TASKS, todas); return dto(tarefa, nome); }
      if (method === 'DELETE' && !m[2]) { gravar(KEY_TASKS, todas.filter((t) => t !== tarefa)); return null; }
    }
    throw new DemoError(404, 'Não encontrado');
  }

  async function cadastrar({ nome, email, senha }) {
    if (!nome || !String(nome).trim() || !email || !String(email).trim() || !senha || !String(senha).trim()) {
      throw new DemoError(400, 'Nome, e-mail e senha são obrigatórios');
    }
    const chave = String(email).trim();
    const usuarios = ler(KEY_USERS, {});
    if (usuarios[chave]) throw new DemoError(409, 'E-mail já cadastrado');
    usuarios[chave] = { nome: String(nome).trim(), senhaHash: await hashSenha(String(senha)) };
    gravar(KEY_USERS, usuarios);

    // três tarefas de exemplo para a demonstração não começar vazia
    const todas = ler(KEY_TASKS, []);
    let id = todas.reduce((m, t) => Math.max(m, t.id), 0);
    [
      ['Exemplo: configurar o CI', 'GitHub Actions rodando os testes a cada push', 'DONE'],
      ['Exemplo: estudar Spring Security', 'Filtro JWT e rotas protegidas', 'DOING'],
      ['Exemplo: publicar o projeto', 'Docker + Render + TiDB Cloud', 'TODO'],
    ].forEach(([titulo, descricao, status]) => todas.push({ id: ++id, dono: chave, titulo, descricao, status }));
    gravar(KEY_TASKS, todas);

    return { id: 1, nome: usuarios[chave].nome, email: chave };
  }

  async function entrar({ email, senha }) {
    const u = ler(KEY_USERS, {})[String(email || '').trim()];
    if (!u) throw new DemoError(401, 'Usuário não encontrado');
    if (!senha || (await hashSenha(String(senha))) !== u.senhaHash) throw new DemoError(401, 'Senha inválida');
    return 'demo.' + encodeURIComponent(String(email).trim());
  }

  window.DevTasksDemo = { handle };
})();
