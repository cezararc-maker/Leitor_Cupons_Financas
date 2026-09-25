# Histórico e edição completa da compra/NF

## Objetivo

Transformar o Histórico em uma área de auditoria e correção dos lançamentos.

O usuário deve conseguir abrir uma compra/NF e entender rapidamente:

- o que foi comprado;
- onde e quando;
- valor bruto;
- descontos;
- valor líquido;
- como foi pago;
- se existe parcelamento;
- quais itens ainda precisam de revisão.

## Navegação

```
Histórico
  ↓
Lista de compras / NFs
  ↓
Selecionar compra
  ↓
Detalhes da compra
```

A lista pode continuar oferecendo filtros e indicadores, mas cada linha/cartão de compra deve ser clicável.

## Detalhes da compra

### Cabeçalho

- estabelecimento;
- CNPJ, quando disponível;
- data;
- origem;
- número/série/chave quando aplicável.

### Totais

- valor bruto;
- desconto total;
- valor líquido.

### Pagamentos

Permitir múltiplas formas:

- PIX;
- Dinheiro;
- Débito;
- Crédito;
- Vale alimentação/refeição;
- Outros.

Cada forma selecionada recebe seu valor.

Crédito pode ser complementado pelo usuário com:

- à vista;
- parcelado;
- quantidade de parcelas.

### Itens

Cada item continua editável individualmente:

- Produto Mestre;
- descrição;
- quantidade;
- unidade;
- preço bruto unitário;
- desconto;
- total líquido;
- taxonomia/revisão.

## Regra importante

Pagamento, parcelamento e desconto global pertencem à **compra/NF**.

Preço, quantidade, Produto Mestre e desconto específico do produto pertencem ao **item**.

Essa separação evita repetir informações da nota em cada produto.

## Status da compra

A tela poderá indicar:

- Completa;
- Precisa de revisão;
- Pagamento incompleto;
- Itens sem Produto Mestre;
- Total de pagamentos divergente.

## Auditoria

Quando um dado importado for alterado pelo usuário, preservar origem e permitir distinguir:

- importado da NFC-e;
- reconhecido por OCR;
- sugerido por IA;
- informado/corrigido pelo usuário.

A implementação detalhada de histórico de alterações pode ser feita em etapa posterior; desde já, o modelo deve evitar sobrescrever silenciosamente dados importantes.
