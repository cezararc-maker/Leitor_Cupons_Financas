# Dados, histórico, vínculos e backup

## Objetivo

Permitir que o histórico de compras cresça sem transformar o aplicativo em um armazenamento permanente de todas as NFC-e, preservando o que realmente precisa sobreviver por longo prazo: o cadastro mestre de produtos e os vínculos aprendidos.

## Separação dos dados

### Cadastro mestre de produtos

Tabela `products`.

Contém o nome normalizado do produto, setor, categoria, subcategoria, unidade, descrição fiscal e observações.

Não deve ser apagada por operações de limpeza do histórico.

### Vínculos aprendidos

Tabela `merchant_product_links`.

Registra a associação aprendida entre:

- CNPJ do estabelecimento;
- código do item no estabelecimento, quando disponível;
- descrição fiscal normalizada;
- produto mestre escolhido pelo usuário.

Essa tabela é independente das NFC-e. Portanto, a exclusão de notas antigas não remove o aprendizado.

Prioridade de reconhecimento:

1. mesmo estabelecimento + mesmo código do item;
2. mesmo estabelecimento + mesma descrição fiscal normalizada;
3. correspondência exata com o cadastro mestre;
4. item permanece não reconhecido para confirmação manual.

### Histórico de compras

Tabelas `receipts` e `receipt_items`.

Guardam notas importadas, datas, estabelecimento, valores e itens. Esses registros poderão ser removidos futuramente por período sem afetar `products` nem `merchant_product_links`.

## Visualização por período

A tela Histórico e Gastos deve oferecer:

- semanal;
- mensal por competência;
- trimestral;
- semestral;
- anual.

As consultas usam `receipts.issuedDate` no formato ISO `AAAA-MM-DD`, permitindo filtros por intervalo sem carregar todo o histórico em memória.

## Backup completo

O backup completo terá finalidade de restauração do aplicativo. Deve preservar, no mínimo:

- produtos;
- vínculos aprendidos;
- configurações relevantes;
- opcionalmente o histórico de compras.

Formato planejado: pacote versionado do aplicativo, independente do XLSX de relatório.

A gravação e restauração devem usar o Storage Access Framework do Android, permitindo que o usuário escolha um provedor disponível no seletor do sistema, inclusive provedores de nuvem instalados no aparelho.

## Exportação XLSX

A planilha é um relatório para consulta fora do aplicativo, não o formato principal de restauração.

Estrutura planejada:

- Resumo;
- Compras;
- Itens;
- Produtos;
- Vínculos.

## Limpeza periódica

A futura função Limpar histórico deverá permitir escolher uma data limite ou período.

Ela poderá excluir:

- itens de NFC-e antigos;
- NFC-e antigas.

Ela nunca deverá excluir automaticamente:

- produtos;
- vínculos aprendidos.

Depois da limpeza, o banco poderá executar manutenção para recuperar espaço físico quando apropriado.
