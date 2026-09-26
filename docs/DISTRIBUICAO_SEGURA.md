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
FIREBASE_GOOGLE_SERVICES_JSON_BASE64
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

## Controle de usuários e dispositivos — base implementada

App Distribution controla quem recebe versões, mas não é suficiente para banir uma instalação que já recebeu o APK.

A base Android/Firestore agora implementa:

- Firebase Authentication por e-mail e senha;
- ausência de cadastro público no aplicativo;
- status de conta `ACTIVE`, `SUSPENDED` e `BLOCKED`;
- dispositivo identificado pelo `installationId` aleatório já existente;
- primeiro dispositivo novo registrado como `PENDING`;
- ativação administrativa separada da autenticação;
- revogação por dispositivo;
- regra offline limitada a 72 horas desde a última validação online aprovada;
- consulta de autorização usando origem `SERVER`, sem depender indefinidamente do cache Firestore;
- regras Firestore que impedem o cliente de alterar o próprio status ou se autoativar.

Estrutura:

```
users/{uid}
  status
  maxDevices

users/{uid}/devices/{installationId}
  installationId
  status
  active
  model
  appVersion
  firstSeenAt
  lastSeenAt
```

Os dados financeiros continuam locais. O Firebase desta camada não recebe compras, itens ou valores.

O limite `maxDevices` é conferido durante a aprovação administrativa. A automação dessa aprovação deverá usar backend confiável/Cloud Function; ela não deve ser delegada ao cliente Android.

Firebase App Check foi integrado no código por variante e agora precisa ser registrado/validado no projeto antes do enforcement.

Decisão atual para a primeira distribuição externa: como ainda não existe conta Google Play Console vinculada ao projeto, o enforcement do App Check ficará adiado. A primeira distribuição continuará por APK no Firebase App Distribution, que não exige Google Play Console. Authentication, regras Firestore e autorização por dispositivo continuam obrigatórias e já foram validadas. O provedor Play Integrity permanece preparado no código para ativação futura. reCAPTCHA Enterprise para Android existe como alternativa, mas está em Preview e não será adotado nesta primeira distribuição.

Detalhes operacionais: `docs/FIREBASE_ACCESS_SETUP.md`.

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

A chave oficial de assinatura e o registro do aplicativo Firebase já foram preparados.

Já concluído e validado no Moto G15:

1. `google-services.json` configurado localmente;
2. Authentication por e-mail/senha habilitado;
3. Firestore criado e `firestore.rules` publicadas;
4. primeira conta autorizada criada;
5. fluxo `PENDING → ACTIVE` validado;
6. suspensão/bloqueio/liberação de conta e revogação/liberação de dispositivo validados;
7. App Check integrado no Android por variante.

Antes da primeira instalação no aparelho de outra pessoa ainda é necessário:

1. configurar o grupo `leitor-cupons-testadores`;
2. configurar os Secrets do GitHub;
3. gerar a primeira versão APK assinada pelo workflow;
4. enviar o convite do App Distribution;
5. validar instalação, login e autorização no primeiro smartphone externo;
6. posteriormente, quando houver Play Console (ou decisão explícita por outro provedor), registrar/validar App Check e só então considerar enforcement.

## Atualização posterior

Uma versão nova deve:

- usar o mesmo `applicationId`;
- usar a mesma chave de assinatura;
- possuir `versionCode` maior;
- manter migrações Room não destrutivas;
- nunca executar `pm clear`;
- nunca substituir o banco por um banco empacotado;
- nunca importar dados de outro usuário automaticamente.
