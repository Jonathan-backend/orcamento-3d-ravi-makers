# Implantação portável

A aplicação continua independente da plataforma: recebe configuração por variáveis de ambiente, usa PostgreSQL externo e escuta a porta indicada por `PORT`.

## Render (destino atual)

O arquivo `render.yaml` cria um Web Service Docker gratuito e configura `/api/public/health` como health check. No primeiro Blueprint, informe:

- `DB_URL`: URL JDBC do PostgreSQL, por exemplo `jdbc:postgresql://host/database?sslmode=require`;
- `DB_USER`: usuário do banco;
- `DB_PASSWORD`: senha do banco;
- `ADMIN_INITIAL_PASSWORD`: senha inicial forte do administrador.

`JWT_SECRET` é gerado pelo próprio Render. Credenciais do Google, se usadas, devem ser adicionadas no painel como `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET`.
`DATA_ENCRYPTION_KEY` também é gerada pelo Render e protege as chaves de API das impressoras no banco. Não altere essa chave depois que houver dados criptografados.

## Saída para Vercel

O `Dockerfile.vercel` manté um caminho de migração sem alterar o backend. Cadastre no Vercel as mesmas variáveis de ambiente e mantenha o PostgreSQL externo.

A limitação que impede uma troca imediata é o limite de 4,5 MB no corpo das requisições das Vercel Functions. Antes de migrar, o upload de G-code deve ser alterado para envio direto a um armazenamento de objetos, seguido da análise pelo backend.

## Contrato comum

| Variável | Uso |
|---|---|
| `PORT` | Porta HTTP fornecida pela plataforma; padrão local `8080` |
| `SPRING_PROFILES_ACTIVE` | Use `prod` na nuvem |
| `DB_URL` | URL JDBC do PostgreSQL |
| `DB_USER` | Usuário do PostgreSQL |
| `DB_PASSWORD` | Senha do PostgreSQL |
| `JWT_SECRET` | Segredo aleatório com pelo menos 48 caracteres |
| `DATA_ENCRYPTION_KEY` | Chave aleatória distinta usada para criptografar credenciais armazenadas |
| `ADMIN_INITIAL_PASSWORD` | Senha usada somente na criação inicial do administrador |

Segredos nunca devem ser incluídos no Git ou em imagens Docker.
