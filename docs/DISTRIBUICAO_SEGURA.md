# Distribuição segura e controle de usuários

## Objetivo

Permitir que o Leitor Cupons Finanças seja instalado em outros smartphones sem conectar os aparelhos ao computador de desenvolvimento, preservando:

- isolamento entre usuários;
- integridade do banco local;
- atualização sem apagar dados;
- assinatura única e permanente do aplicativo;
- distribuição apenas para testadores autorizados;
- futura suspensão/bloqueio remoto de contas e aparelhos;
- ausência de dados reais de usuários dentro do APK.

## Regra fundamental

O APK contém somente código e recursos do aplicativo.

Nunca incluir no APK:

- banco SQLite de usuário;
- backup `.lcfbackup`;
- histórico de compras;
- perfil real;
- credenciais;
- chave privada de assinatura;
- chave de conta de serviço do Firebase.

Cada smartphone cria e mantém seu próprio banco local.

## Proteção contra cópia automática de dados

O Manifest e as regras de extração excluem os dados do aplicativo de:

- backup automático do Android;
- restauração automática;
- transferência direta de dados entre aparelhos.

O backup suportado pelo projeto continua sendo o fluxo explícito `.lcfbackup`.

Antes de liberar backup entre usuários/aparelhos, o formato será evoluído para criptografia e vínculo com a conta autenticada.

## Assinatura oficial

Todas as versões entregues a terceiros devem ser assinadas pela mesma chave privada permanente.

A chave:

- é criada uma única vez;
- nunca entra no Git;
- nunca entra no APK;
- nunca é enviada em conversa;
- deve ter uma cópia de segurança offline;
- no GitHub Actions, será fornecida somente por Secret.

Segredos esperados pelo workflow:

```
LCF_RELEASE_KEYSTORE_BASE64
LCF_RELEASE_KEYSTORE_PASSWORD
LCF_RELEASE_KEY_ALIAS
LCF_RELEASE_KEY_PASSWORD
FIREBASE_APP_ID_ANDROID
FIREBASE_SERVICE_ACCOUNT_BASE64
```

## Firebase App Distribution

O primeiro canal externo será o Firebase App Distribution.

Pacote Android que deve ser registrado no Firebase:

```
br.com.leitorcuponsfinancas
```

Grupo sugerido:

```
leitor-cupons-testadores
```

Somente e-mails explicitamente adicionados ao grupo recebem versões.

O workflow:

```
.github/workflows/distribute-testers.yml
```

executa:

```
testes
  ↓
build release
  ↓
assinatura permanente
  ↓
verificação da assinatura
  ↓
Firebase App Distribution
  ↓
testadores autorizados
```

Nenhum APK é publicado por esse fluxo quando testes ou assinatura falham.

## Controle de usuários — próxima camada obrigatória

App Distribution controla quem recebe versões, mas não é suficiente para banir uma instalação que já recebeu o APK.

Antes da primeira distribuição ampla, integrar:

- Firebase Authentication;
- coleção administrativa de usuários;
- status `ACTIVE`, `SUSPENDED` ou `BLOCKED`;
- dispositivos autorizados por usuário;
- limite configurável de dispositivos;
- revogação de sessão;
- App Check para proteger chamadas ao backend.

Os dados financeiros continuarão locais. O backend de acesso não deve receber compras, itens ou valores apenas para controlar licença/acesso.

Estrutura conceitual:

```
users/{uid}
  status
  maxDevices
  createdAt

users/{uid}/devices/{installationId}
  active
  model
  appVersion
  firstSeenAt
  lastSeenAt
```

O próprio usuário autenticado poderá ler somente seu status/dispositivos. Alterações administrativas de bloqueio devem ser feitas apenas por ambiente administrativo confiável, nunca diretamente pelo cliente Android.

## Atualizações no aplicativo

Em Configurações haverá a área **Atualizações e instalação**.

Comportamento planejado:

```
Nova versão encontrada
        ↓
[ Depois ] [ Atualizar agora ]
        ↓
Depois
        ↓
pergunta novamente na próxima abertura
```

O botão **Verificar atualizações** permanece desabilitado até a integração online estar concluída, evitando transmitir falsa sensação de verificação.

## Modo Desenvolvedor

A instalação por canal normal de testadores não deve exigir Modo Desenvolvedor como regra.

O tutorial aparece somente quando o usuário toca em:

```
Como ativo o modo desenvolvedor?
```

Passos mostrados:

1. Configurações.
2. Sobre o telefone / Informações do software.
3. Número da versão / Número da compilação.
4. Tocar 7 vezes.
5. Confirmar PIN/senha/biometria, se solicitado.
6. Procurar Opções do desenvolvedor.

Depois da instalação, orientar a desativá-lo novamente, principalmente porque apps de segurança, bancos e o gov.br podem restringir o uso com essa opção ativa.

## Primeira instalação externa

Não enviar o APK de desenvolvimento atual.

A primeira instalação no aparelho de outra pessoa somente deve ocorrer depois de:

1. criar a chave oficial de assinatura;
2. registrar o aplicativo no Firebase;
3. configurar o grupo de testadores;
4. configurar os Secrets do GitHub;
5. concluir a camada mínima de autenticação/autorização;
6. gerar a primeira versão assinada pelo workflow;
7. enviar o convite do App Distribution.

## Atualização posterior

Uma versão nova deve:

- usar o mesmo `applicationId`;
- usar a mesma chave de assinatura;
- possuir `versionCode` maior;
- manter migrações Room não destrutivas;
- nunca executar `pm clear`;
- nunca substituir o banco por um banco empacotado;
- nunca importar dados de outro usuário automaticamente.
