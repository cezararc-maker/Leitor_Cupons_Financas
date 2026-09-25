# Validação e integridade financeira

## Princípio

O Leitor Cupons Finanças deve priorizar a **fidedignidade dos dados**. Nenhuma compra, NF, lançamento manual ou correção pode ser gravada quando houver inconsistência financeira comprovável.

A validação não pode existir apenas na tela. A regra deve ser repetida na camada de domínio/persistência para impedir gravação inválida mesmo que uma tela deixe de validar algum campo.

## Experiência de erro

### Campo inválido

O campo deve:

- receber contorno vermelho;
- manter o valor digitado para correção;
- mostrar mensagem objetiva abaixo do campo;
- possuir descrição acessível para leitores de tela.

Exemplo:

```
Total dos pagamentos
R$ 178,00        ← contorno vermelho

Os pagamentos somam R$ 2,00 a menos que
o total líquido da compra (R$ 180,00).
```

### Tentativa de salvar

Quando houver erro impeditivo:

1. não gravar nenhum dado;
2. abrir diálogo com resumo do primeiro erro ou dos principais erros;
3. explicar o motivo;
4. indicar uma correção possível;
5. após **OK**, rolar a tela e mover foco para o primeiro campo com erro.

Exemplo:

```
Não foi possível salvar

A soma das formas de pagamento é R$ 178,00,
mas o total da compra é R$ 180,00.

Revise os valores de PIX, Crédito, Débito,
Dinheiro ou Vale alimentação/refeição.

[ OK ]
```

Ao fechar, o foco vai para o primeiro pagamento inconsistente.

## Tipos de validação

### Erro impeditivo

Impede a gravação.

Exemplos:

- quantidade <= 0;
- preço < 0;
- desconto < 0;
- desconto maior que o valor bruto ao qual se aplica;
- percentual de desconto < 0% ou > 100%;
- valor líquido negativo;
- crédito parcelado com parcelas < 2;
- pagamento com valor negativo;
- soma dos pagamentos divergente do total líquido além da tolerância;
- total manual matematicamente incompatível com quantidade, preço e desconto;
- campo obrigatório ausente.

### Advertência / pendência de revisão

Não é possível afirmar que o dado está errado, mas ele precisa de atenção.

Exemplos:

- documento informa apenas "Cartão" sem distinguir débito/crédito;
- NFC-e informa crédito, mas não informa parcelamento;
- OCR identificou valor com baixa confiança;
- item ainda não possui Produto Mestre;
- desconto global não pode ser atribuído com segurança a itens específicos.

Advertências devem aparecer na Central de Revisão e no status da compra, mas não devem ser convertidas em valores inventados.

## Validação dos itens

### Sem desconto

```
bruto = quantidade × preço_unitário_bruto
líquido = bruto
```

### Desconto por unidade

```
bruto = quantidade × preço_unitário_bruto
desconto_total_item = quantidade × desconto_unitário
líquido = bruto - desconto_total_item
```

### Desconto percentual por unidade

```
desconto_unitário = preço_unitário_bruto × percentual / 100
desconto_total_item = quantidade × desconto_unitário
líquido = bruto - desconto_total_item
```

### Desconto total do item

```
bruto = quantidade × preço_unitário_bruto
líquido = bruto - desconto_total_item
```

Os cálculos monetários devem usar precisão decimal apropriada. Não usar `Double` como regra financeira de persistência quando houver risco de erro de arredondamento.

## Validação da compra/NF

A validação do total não deve considerar somente itens e desconto. O modelo precisa admitir componentes legítimos da compra, como:

- itens;
- descontos;
- frete/entrega;
- acréscimos;
- outras despesas/taxas;
- arredondamentos autorizados/documentados.

Conceitualmente:

```
total_líquido =
    total_bruto_itens
  - descontos_itens
  - desconto_global
  + frete
  + acréscimos
  + outras_despesas
  ± ajuste_de_arredondamento
```

Não criar erro falso quando o documento possuir um componente legítimo que ainda não foi modelado.

## Validação dos pagamentos

Uma compra pode ter múltiplas formas de pagamento.

```
total_pagamentos =
    PIX
  + Dinheiro
  + Débito
  + Crédito
  + Vale alimentação/refeição
  + Outros
```

A soma deve corresponder ao total líquido da compra dentro de uma tolerância monetária explícita e pequena, usada apenas para arredondamento legítimo.

A tolerância nunca deve mascarar diferenças relevantes.

## Parcelamento

- parcelamento pertence ao pagamento em Crédito;
- se "Parcelado = Sim", quantidade de parcelas deve ser >= 2;
- se "Crédito à vista", parcelas = 1 ou nulo conforme modelo final;
- ausência de informação fiscal não deve ser transformada automaticamente em "à vista";
- quando o usuário complementar a informação, registrar origem `USER`.

## Transação e persistência

Salvar uma compra deve ser uma operação atômica:

```
validar
↓
iniciar transação
↓
salvar compra
↓
salvar pagamentos
↓
salvar itens
↓
salvar vínculos/aprendizado
↓
commit
```

Se qualquer etapa falhar:

```
rollback
↓
nenhuma parte fica gravada
```

Isso evita notas parcialmente cadastradas.

## Edição pelo Histórico

Ao editar uma NF/compra existente:

1. carregar uma cópia editável;
2. validar todas as alterações;
3. somente substituir os dados persistidos após validação completa;
4. se falhar, manter intacta a versão anterior.

## Segurança de dados

Integridade e confidencialidade são requisitos diferentes.

O backup atual possui validação de integridade por hash, útil para detectar corrupção. Isso não equivale a criptografia de confidencialidade.

Antes de tratarmos backup como proteção completa de dados sensíveis, deve existir uma etapa específica para avaliar:

- criptografia do backup;
- uso do Android Keystore para chaves locais;
- proteção de dados sensíveis armazenados;
- política de logs sem dados financeiros/identificadores desnecessários;
- exportações sem exposição acidental de dados;
- restauração segura e validação de origem.

## Testes obrigatórios

Cada regra financeira deve possuir testes unitários para:

- limite válido;
- valor inválido;
- arredondamento;
- pagamento misto;
- desconto percentual;
- desconto unitário;
- desconto total;
- crédito parcelado;
- rollback em falha de persistência.

Fluxos de interface devem ser testados para confirmar:

- contorno vermelho;
- mensagem correta;
- diálogo explicativo;
- foco no primeiro campo inválido;
- bloqueio efetivo da gravação.
