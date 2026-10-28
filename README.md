# ReqTrace Monitor

Ferramenta de apoio à rastreabilidade de requisitos. Permite cadastrar requisitos funcionais, não funcionais e regras de negócio, relacioná-los, associar testes, visualizar o modelo em lista, matriz de rastreabilidade e grafo, acompanhar a cobertura de testes, analisar o impacto de alterações e exclusões, gerar, comparar e restaurar versões do modelo e compartilhar o projeto com outros usuários.

O sistema é composto por três partes:

| Parte | Tecnologia | Porta |
|---|---|---|
| Banco de dados | PostgreSQL 16 (em Docker) | 5433 |
| Backend (API) | Java 21 + Spring Boot 4.1 + Maven | 8080 |
| Frontend | React 19 + Vite 8 + TypeScript | 5173 |

## 1. Pré-requisitos

Instale os programas abaixo antes de começar. As versões indicadas são as mínimas recomendadas; entre parênteses, a versão com que o projeto foi desenvolvido e testado.

| Programa | Versão | Para que serve |
|---|---|---|
| Git | 2.40 ou superior (2.41) | Baixar o código |
| JDK (Java) | 21 (21.0.11 LTS) | Compilar e executar o backend |
| Apache Maven | 3.9 ou superior (3.9.9) | Gerenciar as dependências e executar o backend |
| Docker Desktop | 4.x, com Docker Engine 27 ou superior (27.2.0) | Executar o banco de dados PostgreSQL |
| Node.js | 22 LTS ou superior (24.19.0) | Executar o frontend |
| npm | 10 ou superior (10.1.0), instalado junto com o Node.js | Instalar as dependências do frontend |

Para conferir se tudo está instalado, abra um terminal e execute:

```bash
git --version
```

```bash
java -version
```

```bash
mvn -v
```

```bash
docker --version
```

```bash
node -v
```

```bash
npm -v
```

O comando `java -version` deve mostrar a versão 21, e `mvn -v` deve indicar que o Maven está usando esse mesmo Java. Se aparecer outra versão de Java, ajuste a variável de ambiente `JAVA_HOME` para a pasta do JDK 21.

Observações para Windows:

- Use um caminho de pasta sem caracteres especiais (por exemplo, sem `&`). Alguns comandos do npm falham em caminhos com esses caracteres.
- O Docker Desktop precisa estar aberto sempre que o sistema for usado, pois é ele que executa o banco de dados.

## 2. Baixar o projeto

```bash
git clone https://github.com/lryanborges/V-VMonitor.git vvmonitor
```

```bash
cd vvmonitor
```

A estrutura de pastas é:

```
vvmonitor/
  backend/    API em Spring Boot e configuração do banco (docker-compose.yml)
  frontend/   Interface em React
  docs/       Documentação complementar (casos de teste e matriz de rastreabilidade)
```

## 3. Iniciar o banco de dados

Com o Docker Desktop aberto, entre na pasta do backend e suba o contêiner do PostgreSQL:

```bash
cd backend
```

```bash
docker compose up -d
```

Esse comando baixa a imagem `postgres:16-alpine` (somente na primeira vez) e cria o contêiner `vvmonitor-postgres`, com:

- banco: `vvmonitor`
- usuário: `vvmonitor`
- senha: `vvmonitor`
- porta no computador: `5433` (a 5432 fica livre para um PostgreSQL instalado localmente)

Os dados ficam guardados no volume `postgres-data` e não se perdem ao desligar o contêiner ou o computador.

Para conferir se o banco está pronto:

```bash
docker ps
```

O contêiner `vvmonitor-postgres` deve aparecer com o estado `healthy`. O contêiner não inicia sozinho junto com o Docker Desktop: depois de reiniciar o computador, execute `docker compose up -d` novamente na pasta `backend`.

As tabelas não precisam ser criadas manualmente: o backend as cria na primeira execução (próximo passo).

## 4. Executar o backend

Ainda na pasta `backend`, execute:

```bash
mvn spring-boot:run
```

Na primeira execução, o Maven baixa as dependências, o que pode levar alguns minutos. Em seguida, o backend:

1. conecta ao banco em `localhost:5433`;
2. cria ou atualiza as tabelas pelas migrações do Flyway (pasta `src/main/resources/db/migration`);
3. fica disponível em `http://localhost:8080`.

Para conferir se o backend está no ar, abra no navegador:

```
http://localhost:8080/actuator/health
```

A resposta deve conter `"status":"UP"`.

Mantenha esse terminal aberto enquanto usar o sistema. Para parar o backend, pressione `Ctrl + C`.

### Configuração (opcional)

O backend já vem configurado para uso local (perfil `dev`, ativo por padrão). Os valores podem ser alterados por variáveis de ambiente, sem mexer no código:

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/vvmonitor` | Endereço do banco |
| `DB_USERNAME` | `vvmonitor` | Usuário do banco |
| `DB_PASSWORD` | `vvmonitor` | Senha do banco |
| `SERVER_PORT` | `8080` | Porta do backend |
| `JWT_SECRET` | segredo de desenvolvimento | Chave usada para assinar o login (mínimo de 32 caracteres) |
| `JWT_EXPIRATION` | `30d` no perfil `dev` | Validade do login |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil de execução |

Fora do perfil `dev`, a variável `JWT_SECRET` é obrigatória e deve ser um valor secreto próprio.

## 5. Executar o frontend

Abra um segundo terminal, a partir da pasta raiz do projeto:

```bash
cd frontend
```

Instale as dependências (somente na primeira vez, ou quando o `package.json` mudar):

```bash
npm install
```

Inicie o frontend:

```bash
npm run dev
```

O frontend fica disponível em:

```
http://localhost:5173
```

O frontend repassa automaticamente as chamadas `/api` para o backend em `http://localhost:8080`, por isso o backend precisa estar em execução. Para apontar para um backend em outra porta, defina a variável `API_TARGET` antes de iniciar (exemplo: `API_TARGET=http://localhost:8081`).

Mantenha esse terminal aberto enquanto usar o sistema. Para parar o frontend, pressione `Ctrl + C`.

## 6. Usar o sistema

1. Acesse `http://localhost:5173`.
2. Selecione "Criar conta" e cadastre-se com nome, e-mail e senha (mínimo de 8 caracteres).
3. Entre com o e-mail e a senha cadastrados.
4. Crie um projeto em "Novo projeto".
5. Dentro do projeto, cadastre requisitos e regras de negócio em "Novo elemento", relacione-os pelo painel lateral, associe testes e alterne entre as visualizações Lista, Grafo e Matriz.
6. Use "Submeter versão" para salvar o estado do modelo e o menu "Versões" para consultar, comparar e restaurar versões.
7. Use o menu "Membros" para compartilhar o projeto com outros usuários já cadastrados.

## 7. Resumo da execução diária

Depois da primeira instalação, para usar o sistema basta:

1. Abrir o Docker Desktop.
2. No terminal 1, na pasta `backend`:

    ```bash
    docker compose up -d
    ```

    ```bash
    mvn spring-boot:run
    ```

3. No terminal 2, na pasta `frontend`:

    ```bash
    npm run dev
    ```

4. Acessar `http://localhost:5173`.

## 8. Testes e verificação do código

Testes automatizados do backend (pasta `backend`):

```bash
mvn test
```

Verificação de tipos e geração da versão de produção do frontend (pasta `frontend`):

```bash
npm run build
```

Análise estática do frontend (pasta `frontend`):

```bash
npm run lint
```

## 9. Parar e remover o banco

Para parar o contêiner do banco, mantendo os dados (pasta `backend`):

```bash
docker compose stop
```

Para remover o contêiner e apagar todos os dados do banco (pasta `backend`). Esta ação não pode ser desfeita:

```bash
docker compose down -v
```

## 10. Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| O backend encerra com `Connection to localhost:5433 refused` | O banco não está em execução | Abra o Docker Desktop e execute `docker compose up -d` na pasta `backend` |
| `docker compose` informa que não encontra o Docker | O Docker Desktop está fechado | Abra o Docker Desktop e aguarde ele terminar de iniciar |
| O frontend mostra erros de carregamento e o terminal do Vite exibe `http proxy error ... ECONNREFUSED` | O backend não está em execução | Inicie o backend (passo 4) |
| O backend encerra com `Schema validation: missing column ...` | O código do backend está desatualizado em relação ao banco (por exemplo, em uma branch antiga) | Atualize o código para a versão mais recente e execute o backend novamente |
| `Port 8080 was already in use` ou porta 5173 ocupada | Outra aplicação usa a porta | Encerre a outra aplicação ou use outra porta (`SERVER_PORT` no backend; `npm run dev -- --port 5174` no frontend, com `API_TARGET` se o backend também mudar) |
| `java -version` mostra uma versão diferente de 21 | Outro JDK está configurado | Ajuste `JAVA_HOME` para o JDK 21 e reabra o terminal |
| `npm install` falha no Windows | Caminho da pasta com caracteres especiais | Mova o projeto para um caminho simples, como `C:\projetos\vvmonitor` |
