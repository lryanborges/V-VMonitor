# Casos de teste e matriz de rastreabilidade

Gerado a partir do projeto de exemplo "ReqTrace Monitor" (conta ryan@gmail.com), versão v2 "Documento de requisitos v1.3". Os códigos dos testes (T1 a T11) são os mesmos do sistema.

## 1. Casos de teste

### T1, Cadastrar conta com dados válidos
**Requisitos cobertos:** RF1, RNF3
**Caso de uso:** UC-01
**Pré-condições:** O e-mail informado ainda não está cadastrado no sistema.
**Passos:**
1. Acessar "Criar conta".
2. Informar nome, e-mail válido, senha com no mínimo 8 caracteres e a mesma senha na confirmação.
3. Selecionar "Cadastrar".
4. Consultar no banco de dados a senha armazenada da conta criada.

**Resultado esperado:** A conta é criada e o sistema redireciona para o login com a confirmação do cadastro. A senha não está armazenada em texto, e sim como hash Bcrypt.

### T2, Cadastrar com e-mail já cadastrado
**Requisitos cobertos:** RF1
**Caso de uso:** UC-01 (exceção do passo 4)
**Pré-condições:** Existe uma conta com o e-mail que será informado.
**Passos:**
1. Acessar "Criar conta".
2. Informar nome, o e-mail já cadastrado, senha e confirmação.
3. Selecionar "Cadastrar".

**Resultado esperado:** O sistema informa que o e-mail já está cadastrado e não cria uma nova conta.

### T3, Entrar com credenciais corretas
**Requisitos cobertos:** RF2
**Caso de uso:** UC-02
**Pré-condições:** O ator possui conta cadastrada.
**Passos:**
1. Acessar a tela de login.
2. Informar o e-mail e a senha corretos.
3. Selecionar "Entrar".

**Resultado esperado:** O ator é autenticado e o sistema apresenta a tela inicial dos projetos.

### T4, Entrar com senha errada
**Requisitos cobertos:** RF2
**Caso de uso:** UC-02 (exceção do passo 4)
**Pré-condições:** O ator possui conta cadastrada.
**Passos:**
1. Acessar a tela de login.
2. Informar o e-mail correto e uma senha errada.
3. Selecionar "Entrar".

**Resultado esperado:** O sistema informa que as credenciais estão erradas e o ator permanece na tela de login.

### T5, Acessar projeto sem estar autenticado
**Requisitos cobertos:** RNF1
**Caso de uso:** —
**Pré-condições:** O ator não está autenticado e conhece o endereço de um projeto.
**Passos:**
1. Abrir diretamente no navegador o endereço de um projeto.

**Resultado esperado:** O sistema não apresenta o projeto e redireciona o ator para a tela de login.

### T6, Criar projeto
**Requisitos cobertos:** RF4
**Caso de uso:** UC-03
**Pré-condições:** O ator está autenticado.
**Passos:**
1. Selecionar "Novo projeto".
2. Informar nome e descrição.
3. Selecionar "Criar projeto".

**Resultado esperado:** O projeto é criado e o sistema apresenta a interface do projeto.

### T7, Cadastrar requisito com prioridade
**Requisitos cobertos:** RF5
**Caso de uso:** UC-05
**Pré-condições:** O ator está autenticado, tem permissão de edição e acessou um projeto.
**Passos:**
1. Selecionar "Novo elemento".
2. Escolher o tipo "Funcional", informar a descrição e escolher a prioridade.
3. Selecionar "Cadastrar".

**Resultado esperado:** O requisito é cadastrado com identificador gerado pelo sistema (ex.: RF1, RF2…), na sequência dos já existentes, e aparece na lista com a prioridade escolhida.

### T8, Cadastrar regra de negócio
**Requisitos cobertos:** RF6
**Caso de uso:** UC-07
**Pré-condições:** O ator está autenticado, tem permissão de edição e acessou um projeto.
**Passos:**
1. Selecionar "Novo elemento".
2. Escolher o tipo "Regra de negócio" e informar a descrição.
3. Selecionar "Cadastrar".

**Resultado esperado:** A regra é cadastrada com identificador gerado pelo sistema (ex.: RN1) e aparece na lista, sem prioridade.

### T9, Relacionar dois requisitos com o tipo Dependência
**Requisitos cobertos:** RF7, RF8
**Caso de uso:** UC-06
**Pré-condições:** O projeto possui ao menos dois elementos.
**Passos:**
1. Selecionar um requisito e, no painel lateral, selecionar "Adicionar".
2. Escolher o outro requisito.
3. Escolher o tipo "Dependência" e confirmar.

**Resultado esperado:** O relacionamento é registrado e aparece nas duas pontas: como "Depende de" no requisito de origem e como "Necessário para" no requisito de destino.

### T10, Editar a descrição de um requisito
**Requisitos cobertos:** RF14
**Caso de uso:** —
**Pré-condições:** O projeto possui ao menos um requisito.
**Passos:**
1. Selecionar um requisito e selecionar "Editar".
2. Alterar a descrição.
3. Selecionar "Salvar alterações".

**Resultado esperado:** A nova descrição aparece na lista e no painel do requisito, que passa a ser indicado como alteração pendente até a próxima versão.

### T11, Excluir requisito com vínculos
**Requisitos cobertos:** RF15, RF16
**Caso de uso:** —
**Pré-condições:** O projeto possui um requisito com relacionamentos.
**Passos:**
1. Selecionar o requisito e selecionar "Excluir".
2. Verificar o alerta apresentado.
3. Confirmar a exclusão (no nível alto, digitando o identificador).

**Resultado esperado:** Antes da exclusão, o sistema apresenta o nível de impacto e os relacionamentos, elementos órfãos, dependentes e testes afetados. Após a confirmação, o requisito e seus relacionamentos são removidos.

## 2. Matriz de rastreabilidade: requisitos × casos de uso × testes

| Requisito | Prioridade | Caso de uso | Testes | Cobertura |
|---|---|---|---|---|
| RF1 | Obrigatório | UC-01 | T1, T2 | Coberto |
| RF2 | Obrigatório | UC-02 | T3, T4 | Coberto |
| RF3 | Obrigatório | UC-04 | — | Sem teste |
| RF4 | Obrigatório | UC-03 | T6 | Coberto |
| RF5 | Obrigatório | UC-05 | T7 | Coberto |
| RF6 | Obrigatório | UC-07 | T8 | Coberto |
| RF7 | Obrigatório | UC-06 | T9 | Coberto |
| RF8 | Desejável | UC-06 | T9 | Coberto |
| RF9 | Opcional | — | — | Sem teste |
| RF10 | Obrigatório | — | — | Sem teste |
| RF11 | Desejável | — | — | Sem teste |
| RF12 | Opcional | — | — | Sem teste |
| RF13 | Opcional | — | — | Sem teste |
| RF14 | Obrigatório | — | T10 | Coberto |
| RF15 | Obrigatório | — | T11 | Coberto |
| RF16 | Obrigatório | — | T11 | Coberto |
| RF18 | Opcional | — | — | Sem teste |
| RF20 | Desejável | — | — | Sem teste |
| RF21 | Opcional | — | — | Sem teste |
| RF22 | Opcional | — | — | Sem teste |
| RNF1 | Obrigatório | — | T5 | Coberto |
| RNF2 | Desejável | — | — | Sem teste |
| RNF3 | Obrigatório | UC-01 (passo 5) | T1 | Coberto |
| RNF4 | Obrigatório | — | — | Sem teste |
| RNF5 | Opcional | — | — | Sem teste |
| RNF6 | Desejável | — | — | Sem teste |
| RNF7 | Desejável | — | — | Sem teste |
| RNF8 | Desejável | — | — | Sem teste |
| RNF9 | Desejável | — | — | Sem teste |
| RNF10 | Obrigatório | — | — | Sem teste |
| RNF11 | Desejável | — | — | Sem teste |

**Cobertura de testes:** 12 de 31 requisitos (39%). RF17 e RF19 foram removidos na v1.3 e não aparecem.

## 3. Matriz de rastreabilidade: relacionamentos entre elementos

Leitura: o elemento da linha se relaciona com os da coluna. As regras de negócio (RN1 a RN7) aparecem como destino.

| Elemento | Depende de | Refina | Similar a | Em conflito com |
|---|---|---|---|---|
| RF1 | RN1, RN2, RNF3 | — | — | — |
| RF2 | RF1 | — | — | — |
| RF3 | RF2 | — | — | — |
| RF4 | RF2 | — | — | — |
| RF5 | RF3, RF4, RN4, RN5 | — | — | — |
| RF6 | RF4, RN4 | — | — | — |
| RF7 | RF5, RF6, RN3 | — | — | — |
| RF8 | — | RF7 | — | — |
| RF9 | RF5, RN7 | — | — | — |
| RF10 | RF5, RF6, RF7 | — | RNF5 | — |
| RF11 | RF10 | — | — | RNF7 |
| RF12 | — | RF11 | — | — |
| RF13 | RF9 | — | — | — |
| RF14 | RF5, RF7 | — | — | — |
| RF15 | RF5 | — | — | — |
| RF16 | — | RF14, RF15 | — | — |
| RF18 | RF10 | — | — | — |
| RF20 | RF1, RF4, RN6, RNF4 | — | — | — |
| RF21 | — | RF11 | — | — |
| RF22 | RF10, RF18 | — | — | — |
| RNF1 | RF2 | — | — | — |
| RNF2 | — | RF10 | — | — |
| RNF3 | — | RNF4 | — | — |
| RNF4 | — | RNF1 | — | — |
| RNF8 | — | RF1, RF4 | — | — |
| RNF9 | — | — | RNF8 | — |

Elementos sem relacionamento: RNF6 e RNF10.

Similaridade e conflito valem nos dois sentidos (RF10 é similar a RNF5 e RNF5 é similar a RF10).
