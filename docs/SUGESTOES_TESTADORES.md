# Central de Sugestões de Testadores

## Objetivo

Canal interno do aplicativo para testadores enviarem ideias, melhorias e dificuldades sem expor credenciais do GitHub.

As solicitações ficam no Cloud Firestore, coleção `feedback`.

## Fluxo do testador

1. Tocar no balão flutuante com `...`.
2. Escolher **Enviar nova sugestão**.
3. Digitar entre 10 e 1500 caracteres.
4. Tocar em **Enviar**.
5. A solicitação nasce com status **Recebido**.
6. Em **Minhas sugestões**, acompanhar o status e, quando aplicável, o motivo do descarte.

## Status

- `RECEIVED` → Recebido
- `IN_REVIEW` → Em Análise
- `IN_DEVELOPMENT` → Em Desenvolvimento
- `TESTING` → Fase de Testes
- `DEPLOYMENT` → Fase de Implantação
- `COMPLETED` → Implantação Concluída
- `DISCARDED` → Descartado

Ao marcar como **Descartado**, o motivo é obrigatório e fica visível ao autor.

## Administrador

A conta administrativa precisa ter no documento `users/{uid}`:

```
role: "ADMIN"
```

O campo é criado manualmente pelo administrador no Firebase. O aplicativo não pode promover uma conta para ADMIN.

A conta ADMIN vê no mesmo balão:

```
Central de solicitações
```

e pode alterar o status de cada item.

## Segurança

Regras do Firestore:

- qualquer usuário autenticado pode criar uma sugestão apenas em seu próprio UID;
- usuário comum só pode ler suas próprias sugestões;
- usuário comum não pode alterar mensagem, autor ou status;
- administrador é identificado por `users/{uid}.role == "ADMIN"`;
- apenas administrador pode alterar o status;
- exclusão pelo cliente é bloqueada;
- o GitHub não recebe token ou credencial dentro do APK.

## Notificações

O app cria um canal Android chamado **Sugestões e melhorias**.

Para o administrador:

- nova sugestão gera notificação quando o listener do app recebe o documento;
- existe contador de não lidas no balão.

Para o testador:

- mudança de status gera notificação;
- ao abrir **Minhas sugestões**, as mudanças são marcadas como vistas;
- motivo de descarte aparece no acompanhamento e na notificação quando disponível.

Limitação atual: as notificações usam o listener do Firestore no próprio app. Quando o processo do aplicativo estiver completamente encerrado pelo Android, a entrega pode ocorrer apenas na próxima abertura. Para push instantâneo com o app totalmente encerrado será necessária uma fase posterior com FCM + backend confiável/Cloud Function.

## Teste antes da distribuição

Antes de lançar aos testadores:

1. publicar `firestore.rules`;
2. adicionar `role = "ADMIN"` à conta do proprietário;
3. atualizar o Moto G15 com a branch atual;
4. aceitar a permissão de notificações;
5. enviar uma sugestão pelo balão;
6. conferir a coleção `feedback` no Firestore;
7. abrir **Central de solicitações**;
8. percorrer os status;
9. validar o motivo obrigatório ao descartar;
10. somente depois gerar nova versão `beta` pelo workflow de distribuição.
