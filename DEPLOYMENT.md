# Implantação portável

A aplicação continua independente da plataforma: recebe configuração por variáveis de ambiente, usa PostgreSQL externo e escuta a porta indicada por `PORT`.

## Vercel (destino atual)

Desde 30/06/2026, a Vercel executa servidores HTTP empacotados em `Dockerfile.vercel` usando Fluid Compute. O contêiner desta aplicação já escuta a porta indicada por `PORT` e pode ser publicado sem reescrever o backend.

1. Importe o repositório no painel da Vercel ou execute `vercel deploy --prod` na raiz.
2. Ative Fluid Compute nas configurações de Functions (projetos novos já o ativam automaticamente).
3. Cadastre as variáveis abaixo para Production e Preview.
4. Mantenha o PostgreSQL em um provedor externo e próximo da região escolhida para a Function.

Variáveis obrigatórias:

- `DB_URL`: URL JDBC do PostgreSQL, por exemplo `jdbc:postgresql://host/database?sslmode=require`;
- `DB_USER`: usuário do banco;
- `DB_PASSWORD`: senha do banco;
- `ADMIN_INITIAL_PASSWORD`: senha inicial forte do administrador;
- `JWT_SECRET`: segredo aleatório com pelo menos 48 caracteres;
- `DATA_ENCRYPTION_KEY`: outro segredo aleatório, diferente do JWT.

Credenciais do Google, se usadas, devem ser adicionadas como `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET`. No Google Cloud, cadastre `https://SEU-DOMINIO/login/oauth2/code/google` como URI de redirecionamento autorizada.

`DATA_ENCRYPTION_KEY` protege as chaves de API das impressoras no banco. Não a altere depois que houver dados criptografados.

## Limitação de upload

A Vercel limita o corpo de cada requisição ou resposta a 4,5 MB, inclusive em contêineres. Arquivos G-code maiores retornarão HTTP 413. Para esses arquivos, a próxima evolução deve usar upload direto para armazenamento de objetos e enviar somente a referência ao backend.

## Contrato comum

| Variável | Uso |
|---|---|
| `PORT` | Configure `8080` na Vercel; padrão local `8080` |
| `SPRING_PROFILES_ACTIVE` | Use `prod` na nuvem |
| `DB_URL` | URL `jdbc:postgresql://`, `postgres://` ou `postgresql://` |
| `DB_POSTGRES_URL` | Criada automaticamente pela integração Neon; tem prioridade sobre `DB_URL` |
| `DB_USER` | Usuário do PostgreSQL |
| `DB_PASSWORD` | Senha do PostgreSQL |
| `JWT_SECRET` | Segredo aleatório com pelo menos 48 caracteres |
| `DATA_ENCRYPTION_KEY` | Chave aleatória distinta usada para criptografar credenciais armazenadas |
| `ADMIN_INITIAL_PASSWORD` | Senha usada somente na criação inicial do administrador |

Segredos nunca devem ser incluídos no Git ou em imagens Docker.
