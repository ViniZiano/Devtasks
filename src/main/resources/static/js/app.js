/* DevTasks — front-end. Fala com a API Spring Boot na mesma origem (sem CORS). */
(() => {
  'use strict';

  const TOKEN_KEY = 'devtasks.token';
  const LABELS = { TODO: 'A fazer', DOING: 'Em andamento', DONE: 'Concluída' };
  const $ = (id) => document.getElementById(id);

  const state = { tasks: [], filter: 'ALL', editingId: null, modo: 'login' };

  /* ---------- token (localStorage pode estar bloqueado: nunca quebra a página) ---------- */
  const store = {
    get() { try { return localStorage.getItem(TOKEN_KEY); } catch { return null; } },
    set(v) { try { localStorage.setItem(TOKEN_KEY, v); } catch { /* sem armazenamento */ } },
    clear() { try { localStorage.removeItem(TOKEN_KEY); } catch { /* sem armazenamento */ } },
  };
  let token = store.get();

  /* ---------- chamada à API ---------- */
  async function api(path, { method = 'GET', body, auth = true } = {}) {
    const headers = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (auth && token) headers['Authorization'] = 'Bearer ' + token;

    let resp;
    try {
      resp = await fetch(path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
    } catch {
      throw new ApiError(0, 'Não consegui falar com o servidor. Verifique sua conexão.');
    }

    if (resp.status === 401 && auth) {      // token vencido (1 h) ou inválido
      logout('Sua sessão expirou. Entre novamente.');
      throw new ApiError(401, 'Sessão expirada');
    }
    if (!resp.ok) throw new ApiError(resp.status, await mensagemDeErro(resp));
    if (resp.status === 204) return null;

    const tipo = resp.headers.get('content-type') || '';
    return tipo.includes('application/json') ? resp.json() : resp.text();
  }

  class ApiError extends Error {
    constructor(status, message) { super(message); this.status = status; }
  }

  async function mensagemDeErro(resp) {
    try {
      const texto = await resp.text();
      try { return JSON.parse(texto).message || padrao(resp.status); } catch { return texto || padrao(resp.status); }
    } catch { return padrao(resp.status); }
  }
  function padrao(status) {
    if (status === 409) return 'Este e-mail já está cadastrado.';
    if (status === 400) return 'Confira os dados informados.';
    if (status === 404) return 'Não encontrado.';
    return 'Algo deu errado. Tente novamente.';
  }

  /* ---------- telas ---------- */
  function mostrar(view) {
    $('authView').hidden = view !== 'auth';
    $('appView').hidden = view !== 'app';
  }
  function erro(el, msg) { el.textContent = msg || ''; el.hidden = !msg; }

  function logout(aviso) {
    token = null;
    store.clear();
    state.tasks = [];
    mostrar('auth');
    setModo('login');
    erro($('authError'), aviso || '');
  }

  /* ---------- login / cadastro ---------- */
  function setModo(modo) {
    state.modo = modo;
    const cad = modo === 'cadastro';
    $('nomeField').hidden = !cad;
    $('authTitle').textContent = cad ? 'Crie sua conta' : 'Entre na sua conta';
    $('authSubtitle').textContent = cad
      ? 'Leva menos de um minuto. Suas tarefas ficam só com você.'
      : 'Suas tarefas ficam salvas e só você consegue vê-las.';
    $('authSubmit').textContent = cad ? 'Criar conta' : 'Entrar';
    $('switchText').textContent = cad ? 'Já tem conta?' : 'Ainda não tem conta?';
    $('switchBtn').textContent = cad ? 'Entrar' : 'Criar conta';
    $('senha').autocomplete = cad ? 'new-password' : 'current-password';
    erro($('authError'), '');
  }

  async function entrar(email, senha) {
    const t = await api('/auth/login', { method: 'POST', body: { email, senha }, auth: false });
    token = String(t).trim();
    store.set(token);
  }

  $('switchBtn').addEventListener('click', () => setModo(state.modo === 'login' ? 'cadastro' : 'login'));

  $('authForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const nome = $('nome').value.trim();
    const email = $('email').value.trim();
    const senha = $('senha').value;
    const cadastro = state.modo === 'cadastro';

    if (!email || !senha || (cadastro && !nome)) {
      return erro($('authError'), 'Preencha todos os campos.');
    }
    if (cadastro && senha.length < 6) {
      return erro($('authError'), 'Use uma senha com pelo menos 6 caracteres.');
    }

    erro($('authError'), '');
    const botao = $('authSubmit');
    botao.disabled = true;
    try {
      if (cadastro) await api('/usuarios', { method: 'POST', body: { nome, email, senha }, auth: false });
      await entrar(email, senha);
      $('authForm').reset();
      await abrirApp();
    } catch (err) {
      // mensagem genérica no login: não revela se o e-mail existe
      erro($('authError'), !cadastro && err.status === 401 ? 'E-mail ou senha incorretos.' : err.message);
    } finally {
      botao.disabled = false;
    }
  });

  /* ---------- app ---------- */
  async function abrirApp() {
    mostrar('app');
    $('today').textContent = new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' });
    try {
      const eu = await api('/usuarios/me');
      const primeiro = (eu.nome || eu.email).split(' ')[0];
      $('userName').textContent = primeiro;
      $('avatar').textContent = primeiro.charAt(0).toUpperCase();
      await carregar();
    } catch (err) {
      if (err.status !== 401) erro($('appError'), err.message);
    }
  }

  async function carregar() {
    erro($('appError'), '');
    state.tasks = await api('/tarefas');
    render();
  }

  function render() {
    const lista = $('taskList');
    lista.replaceChildren();
    const visiveis = state.tasks.filter((t) => state.filter === 'ALL' || t.status === state.filter);

    visiveis.forEach((t) => lista.appendChild(linha(t)));

    const vazio = $('empty');
    vazio.hidden = visiveis.length > 0;
    vazio.textContent = state.tasks.length === 0
      ? 'Nenhuma tarefa ainda. Clique em “Nova tarefa” para começar.'
      : 'Nenhuma tarefa neste filtro.';

    const total = state.tasks.length;
    const feitas = state.tasks.filter((t) => t.status === 'DONE').length;
    const andamento = state.tasks.filter((t) => t.status === 'DOING').length;
    const taxa = total ? Math.round((feitas / total) * 100) : 0;
    $('total').textContent = total;
    $('done').textContent = feitas;
    $('doing').textContent = andamento;
    $('rate').textContent = taxa + '%';
    $('rateBar').style.width = taxa + '%';
  }

  // monta com textContent (nada do que o usuário digita vira HTML)
  function linha(t) {
    const row = document.createElement('div');
    row.className = 'task-row' + (t.status === 'DONE' ? ' is-done' : '');

    const check = document.createElement('button');
    check.type = 'button';
    check.className = 'check' + (t.status === 'DONE' ? ' done' : '');
    check.textContent = t.status === 'DONE' ? '✓' : '';
    check.setAttribute('aria-label', t.status === 'DONE' ? 'Marcar como a fazer' : 'Marcar como concluída');
    check.addEventListener('click', () => alternar(t));

    const info = document.createElement('button');
    info.type = 'button';
    info.className = 'task-info';
    const titulo = document.createElement('strong');
    titulo.textContent = t.titulo;
    const sub = document.createElement('small');
    sub.textContent = t.descricao ? t.descricao : LABELS[t.status];
    info.append(titulo, sub);
    info.addEventListener('click', () => abrirDialogo(t));

    const tag = document.createElement('span');
    tag.className = 'tag ' + t.status;
    tag.textContent = LABELS[t.status] || t.status;

    row.append(check, info, tag);
    return row;
  }

  async function alternar(t) {
    const novo = t.status === 'DONE' ? 'TODO' : 'DONE';
    try {
      const atualizada = await api(`/tarefas/${t.id}/status`, { method: 'PATCH', body: { status: novo } });
      state.tasks = state.tasks.map((x) => (x.id === t.id ? atualizada : x));
      render();
    } catch (err) {
      if (err.status !== 401) erro($('appError'), err.message);
    }
  }

  document.querySelectorAll('.filter').forEach((btn) => btn.addEventListener('click', () => {
    document.querySelectorAll('.filter').forEach((b) => b.classList.remove('active'));
    btn.classList.add('active');
    state.filter = btn.dataset.filter;
    render();
  }));

  /* ---------- diálogo criar / editar ---------- */
  const dialog = $('taskDialog');

  function abrirDialogo(tarefa) {
    state.editingId = tarefa ? tarefa.id : null;
    $('dialogTitle').textContent = tarefa ? 'Editar tarefa' : 'Nova tarefa';
    $('titulo').value = tarefa ? tarefa.titulo : '';
    $('descricao').value = tarefa && tarefa.descricao ? tarefa.descricao : '';
    $('statusSelect').value = tarefa ? tarefa.status : 'TODO';
    $('deleteBtn').hidden = !tarefa;
    erro($('dialogError'), '');
    dialog.showModal();
    $('titulo').focus();
  }

  $('newTaskBtn').addEventListener('click', () => abrirDialogo(null));
  $('cancelBtn').addEventListener('click', () => dialog.close());

  $('taskForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const titulo = $('titulo').value.trim();
    if (!titulo) return erro($('dialogError'), 'Dê um título para a tarefa.');

    const corpo = { titulo, descricao: $('descricao').value.trim(), status: $('statusSelect').value };
    const botao = $('saveBtn');
    botao.disabled = true;
    try {
      if (state.editingId) {
        const nova = await api(`/tarefas/${state.editingId}`, { method: 'PUT', body: corpo });
        state.tasks = state.tasks.map((x) => (x.id === nova.id ? nova : x));
      } else {
        const nova = await api('/tarefas', { method: 'POST', body: corpo });
        state.tasks = [nova, ...state.tasks];
      }
      dialog.close();
      render();
    } catch (err) {
      if (err.status !== 401) erro($('dialogError'), err.message);
    } finally {
      botao.disabled = false;
    }
  });

  $('deleteBtn').addEventListener('click', async () => {
    if (!state.editingId || !confirm('Excluir esta tarefa?')) return;
    try {
      await api(`/tarefas/${state.editingId}`, { method: 'DELETE' });
      state.tasks = state.tasks.filter((x) => x.id !== state.editingId);
      dialog.close();
      render();
    } catch (err) {
      if (err.status !== 401) erro($('dialogError'), err.message);
    }
  });

  $('logoutBtn').addEventListener('click', () => logout(''));

  /* ---------- início ---------- */
  if (token) {
    abrirApp();
  } else {
    mostrar('auth');
    setModo('login');
  }
})();
