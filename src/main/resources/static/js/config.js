/*
 * Endereço da API do DevTasks.
 *  - ''  (vazio): o site e a API estão no mesmo endereço (rodando o Spring Boot). É o padrão.
 *  - 'https://seu-servico.onrender.com': o site é hospedado à parte (GitHub Pages) e conversa com a API publicada.
 * Sem endereço, o site no GitHub Pages abre em modo demonstração (dados só no navegador).
 * Este arquivo é a única diferença entre a cópia do Spring (src/main/resources/static) e a da raiz (GitHub Pages).
 */
window.DEVTASKS_CONFIG = { apiUrl: '' };
