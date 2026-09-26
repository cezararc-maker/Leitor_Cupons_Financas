# Firebase — autenticação e controle de acesso

Este documento descreve a camada mínima de acesso remoto do **Leitor Cupons Finanças**.

## Princípio

O Firebase desta fase controla somente:

- autenticação;
- status da conta;
- aparelhos autorizados;
- versão do aplicativo usada pelo aparelho;
- datas de primeira/última validação.

O Firebase **não recebe compras, itens, valores, histórico financeiro nem o banco Room local**.

## Arquivo `google-services.json`

O arquivo baixado do Firebase deve existir somente na máquina/CI em:

```
app/google-services.json
```

Ele continua ignorado pelo Git e nunca deve ser enviado ao repositório ou ao chat.

A configuração Gradle aplica o plugin Google Services somente quando esse arquivo existe. Isso permite que o CI comum compile o projeto sem armazenar o arquivo no Git.

## SDKs adicionados

- Google Services plugin `4.5.0`;
- Firebase BoM `34.19.0`;
- Firebase Authentication;
- Cloud Firestore.

Nenhum SDK de compras/dados financeiros foi adicionado.

## Método de login inicial

O método adotado para os primeiros testadores é **e-mail + senha**.

O aplicativo não oferece cadastro público.

Procedimento administrativo:

1. habilitar **Authentication > Sign-in method > Email/Password** no Firebase;
2. criar manualmente a conta do testador no console do Firebase Authentication;
3. copiar o `UID` criado;
4. criar o documento Firestore `users/{uid}`;
5. definir:
   - `status = "ACTIVE"`;
   - `maxDevices = 1` (ou outro limite administrativo desejado).

Uma conta criada no Authentication sem documento `users/{uid}` não recebe acesso ao aplicativo.

## Estrutura Firestore

```
users/{uid}
  status: "ACTIVE" | "SUSPENDED" | "BLOCKED"
  maxDevices: number

users/{uid}/devices/{installationId}
  installationId: string
  status: "PENDING" | "ACTIVE" | "REVOKED"
  active: boolean
  model: string
  appVersion: string
  firstSeenAt: timestamp
  lastSeenAt: timestamp
```

O `installationId` é aleatório e criado pelo próprio aplicativo. Não é IMEI.

## Primeiro login em um aparelho

Quando a conta está `ACTIVE` e aquele `installationId` ainda não existe:

1. o aplicativo cria o dispositivo com:
   - `status = "PENDING"`;
   - `active = false`;
2. o aplicativo bloqueia o uso;
3. o administrador confere o aparelho e o limite `maxDevices`;
4. se aprovado, altera no Firestore:
   - `status = "ACTIVE"`;
   - `active = true`;
5. o testador toca em **Tentar novamente**.

O cliente Android não pode ativar a si próprio pelas regras do Firestore.

## Suspensão e bloqueio

Para suspender temporariamente uma conta:

```
users/{uid}.status = "SUSPENDED"
```

Para bloquear:

```
users/{uid}.status = "BLOCKED"
```

Para revogar apenas um aparelho:

```
users/{uid}/devices/{installationId}.status = "REVOKED"
users/{uid}/devices/{installationId}.active = false
```

## Regra offline

Uma release autorizada pode continuar funcionando sem internet por no máximo **72 horas** desde a última validação online bem-sucedida da mesma conta e da mesma instalação.

Regras:

- uma resposta online `SUSPENDED`, `BLOCKED` ou aparelho revogado bloqueia imediatamente;
- sem internet, o app nunca usa cache Firestore para prolongar indefinidamente o acesso;
- após 72 horas, uma nova validação online é obrigatória;
- retrocesso do relógio local não renova a janela offline.

Esta é uma primeira proteção prática. Firebase App Check permanece como próxima camada de endurecimento.

## Builds de desenvolvimento x release

### Debug normal

```
REMOTE_ACCESS_REQUIRED = false
```

O desenvolvimento local continua funcionando sem login.

### Debug para testar autenticação no celular

```powershell
.\gradlew.bat assembleDebug -PLCF_REMOTE_ACCESS_REQUIRED=true
```

O script `scripts/atualizar-app-celular.ps1` também aceita `-TestRemoteAccess`.

### Release

Toda release define obrigatoriamente:

```
REMOTE_ACCESS_REQUIRED = true
```

Portanto a versão distribuída a testadores não possui bypass de autenticação.

## Regras Firestore

O repositório contém:

```
firestore.rules
firebase.json
```

As regras permitem ao usuário autenticado:

- ler somente seu próprio documento;
- ler seus próprios dispositivos;
- criar apenas uma solicitação de aparelho `PENDING`;
- atualizar somente `model`, `appVersion` e `lastSeenAt`.

O cliente não pode:

- alterar `ACTIVE / SUSPENDED / BLOCKED`;
- aumentar `maxDevices`;
- tornar um aparelho ativo;
- revogar/desrevogar um aparelho;
- escrever dados de outro usuário.

Alterações administrativas devem ocorrer pelo Console Firebase ou por backend confiável/Admin SDK.

## App Distribution

Para uma release via GitHub Actions serão esperados:

```
LCF_RELEASE_KEYSTORE_BASE64
LCF_RELEASE_KEYSTORE_PASSWORD
LCF_RELEASE_KEY_ALIAS
LCF_RELEASE_KEY_PASSWORD
FIREBASE_APP_ID_ANDROID
FIREBASE_SERVICE_ACCOUNT_BASE64
FIREBASE_GOOGLE_SERVICES_JSON_BASE64
```

O workflow recria os arquivos protegidos apenas no runner temporário.

Grupo previsto:

```
leitor-cupons-testadores
```

## Próximas camadas

Antes de ampliar a distribuição:

1. habilitar Authentication Email/Password;
2. criar Firestore e publicar `firestore.rules`;
3. criar o primeiro usuário administrativo/testador;
4. validar o fluxo PENDING → ACTIVE no Moto G15;
5. criar o grupo `leitor-cupons-testadores`;
6. configurar os Secrets do GitHub;
7. habilitar Firebase App Check;
8. depois evoluir a aprovação de dispositivos para backend confiável/Cloud Function, caso se deseje aplicação automática de `maxDevices`;
9. criptografar/vincular o `.lcfbackup` ao usuário antes de permitir transporte entre aparelhos.
