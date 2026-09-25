# Roadmap e estado do projeto

Este documento é a referência principal para saber **onde o projeto está**, **o que já foi concluído** e **qual é a ordem planejada das próximas etapas**.

> Regra de trabalho: o GitHub é a fonte oficial do projeto. Mudanças relevantes de arquitetura, fluxo ou prioridade devem ser refletidas neste documento para evitar perda de contexto entre sessões.

## Estado atual

Branch de desenvolvimento:

`feat/android-ms-mvp`

### Concluído / funcional

- Projeto Android em Kotlin + Jetpack Compose.
- Persistência local com Room/SQLite.
- Leitura de QR Code / chave NFC-e e consulta pública.
- Lançamento manual.
- Histórico de compras e itens.
- Produtos Mestres.
- Vínculos/aliases aprendidos por estabelecimento.
- Estabelecimento Mestre.
- Taxonomia hierárquica:
  - Segmento;
  - Departamento;
  - Categoria;
  - Subcategoria;
  - Produto Mestre.
- Segmento do estabelecimento restringindo os ramos disponíveis da taxonomia.
- Cadastro, edição e reaproveitamento de classificações.
- Importação incremental de taxonomia em TXT.
- Exportação de taxonomia em TXT.
- Manual do layout de importação.
- Central de Revisão para itens sem Produto Mestre.
- Alerta visual de itens que precisam de revisão.
- Filtros e resumo analítico no Histórico.
- Dashboard com indicadores mensais.
- Ranking de estabelecimentos/categorias/produtos.
- Comparação de preços por Produto Mestre.
- Tema claro, escuro e seguir sistema.
- Paletas e degradê.
- Controle de escala da fonte.
- Tutorial guiado.
- Navegação por gestos entre áreas principais.
- Menu central `+` animado.
- Entrada monetária com centavos automáticos.
- Campo de data com máscara e calendário.
- Backup restaurável `.lcfbackup`, com validação e integridade.
- Testes automatizados e GitHub Actions.
- Emulador de referência: `LeitorCupons_API34_Lite`.

## Decisões de produto já definidas

### Produto Mestre

Produto Mestre representa o **item raiz**, independentemente de marca.

Exemplos:

- Macarrão Renata -> Macarrão.
- Macarrão Liane -> Macarrão.
- Macarrão instantâneo permanece separado de macarrão comum.

Marcas, descrições fiscais, códigos internos, GTINs e variações ficam como aliases/vínculos aprendidos.

### Taxonomia

Estrutura:

```
Segmento do estabelecimento
        ↓
Departamento
        ↓
Categoria
        ↓
Subcategoria
        ↓
Produto Mestre
        ↓
Aliases / descrições fiscais / códigos
```

O Segmento pertence ao estabelecimento. Um Produto Mestre pode participar de mais de um ramo/segmento sem ser duplicado.

Exemplo:

```
Mercado
→ Alimentos
→ Hortifruti
→ Frutas
→ Banana
```

A árvore apresentada ao usuário deve sempre respeitar o caminho anterior. Um estabelecimento classificado como Farmácia não deve oferecer ramos exclusivos de Mercado, e vice-versa.

### Importação da taxonomia

Formato principal: TXT UTF-8 separado por ponto e vírgula.

A importação é **sempre incremental**:

- não apagar;
- não sobrescrever automaticamente;
- preservar classificações existentes;
- reutilizar Produtos Mestres existentes quando possível;
- acrescentar somente os novos nós e vínculos válidos.

Manual: `docs/taxonomia-importacao.txt`.

### Dados financeiros

A base real passa a ser construída daqui para frente. Não serão criados dados históricos artificiais apenas para preencher comparativos.

Quando não houver histórico suficiente, a interface deve informar isso claramente.

### Descontos

Os descontos fazem parte do histórico financeiro e devem ser preservados em dois níveis:

- **por item**, quando o documento informar desconto individual do produto;
- **total da compra**, quando houver desconto global ou totalização fiscal.

Para análises, o app deve manter separados:

- valor bruto;
- desconto;
- valor líquido efetivamente pago/atribuído ao item ou compra.

Isso permitirá medir:

- economia obtida por descontos;
- produtos/categorias com mais desconto;
- percentual médio de desconto;
- estabelecimentos que mais concederam descontos;
- diferença entre preço bruto e preço efetivo;
- evolução do preço líquido do Produto Mestre.

No lançamento manual, o usuário também poderá informar desconto por item e/ou desconto total.

### Forma de pagamento e parcelamento

Cada compra deve permitir registrar **uma ou mais formas de pagamento**, porque uma mesma compra pode ser dividida entre meios diferentes.

Formas iniciais:

- PIX;
- Dinheiro;
- Débito;
- Crédito;
- Outros.

Para Crédito, registrar também:

- à vista ou parcelado;
- quantidade de parcelas;
- valor total associado àquela forma de pagamento;
- futuramente, cartão/conta utilizado e competência de cobrança.

A modelagem não deve limitar a compra a um único campo `paymentMethod`. O desenho previsto usa registros de pagamento vinculados à compra, permitindo pagamento misto.

Esses dados alimentarão análises como:

- distribuição dos gastos por forma de pagamento;
- percentual pago no crédito;
- percentual parcelado;
- quantidade e valor de compras parceladas;
- compromissos futuros de parcelas, quando houver informação suficiente de vencimento/cartão;
- comparação entre consumo realizado e fluxo financeiro;
- filtros de histórico por forma de pagamento.

A captura poderá vir de NFC-e, OCR/IA ou confirmação manual. A forma de pagamento deve ser importada automaticamente quando estiver disponível no documento fiscal. Quando o documento não trouxer parcelamento de forma confiável, o app deve pedir confirmação do usuário.

### Edição da compra / nota no Histórico

O Histórico deve permitir abrir uma compra/NF como uma entidade completa, e não apenas navegar por itens.

Ao tocar em uma nota/compra, abrir uma tela de detalhes com:

- dados da compra/NF;
- estabelecimento;
- data;
- número/chave quando existirem;
- valor bruto;
- desconto total;
- valor líquido;
- formas de pagamento;
- parcelamento;
- itens;
- pendências de revisão.

A edição da compra/NF deve permitir:

- corrigir dados cadastrais da nota;
- adicionar, remover ou ajustar formas de pagamento;
- registrar pagamento misto;
- complementar parcelamento quando a nota não trouxer essa informação;
- registrar desconto total da compra;
- abrir cada item para corrigir Produto Mestre, quantidade, preço e desconto por item.

As formas de pagamento pertencem à compra/NF e **não** devem ser repetidas em cada item.

Exemplo:

```
NF 12345 — Total R$ 180,00

PIX ................ R$ 60,00
Crédito ............ R$ 80,00 em 2x
Vale alimentação ... R$ 40,00
```

A tela deve validar que a soma dos pagamentos corresponda ao total líquido da compra.

### Padrão de ações na interface

Foi definido um padrão visual reutilizável:

- ações positivas/finais (`Confirmar`, `Criar`, `Vincular`, `OK`, `Prosseguir`) usam botão primário preenchido e acompanham a cor do tema;
- ações neutras (`Cancelar`, `Fechar`, `Voltar`) usam estilo neutro;
- ações destrutivas usam semântica de erro e confirmação;
- sugestões inteligentes aceitas pelo botão `Usar <Produto>` devem vincular diretamente, sem obrigar o usuário a percorrer novamente a taxonomia;
- em cadastro/vinculação manual, a ação final só é habilitada quando o preenchimento necessário estiver completo.

### Integridade financeira e bloqueio de gravação

Por se tratar de controle financeiro, o aplicativo não deve gravar dados comprovadamente inconsistentes.

A validação deve existir em duas camadas:

1. **interface**, para orientar o usuário imediatamente;
2. **regra de domínio/persistência**, para impedir que uma falha de interface grave informação inválida.

Comportamento padrão ao tentar salvar dados inválidos:

- destacar em vermelho cada campo com erro;
- exibir mensagem curta junto ao campo;
- quando houver erro relevante ou múltiplos erros, abrir uma janela explicando:
  - o que está errado;
  - por que isso impede a gravação;
  - uma correção possível;
- após o usuário tocar em **OK**, rolar a tela e mover o foco para o primeiro campo inválido;
- manter o botão Salvar bloqueado enquanto existir erro impeditivo;
- nunca fazer salvamento parcial de uma compra/NF.

Exemplos de erros impeditivos:

- pagamentos somados diferentes do total líquido da compra, fora da tolerância definida;
- desconto maior que o valor bruto;
- quantidade menor ou igual a zero;
- preço negativo;
- crédito marcado como parcelado sem quantidade válida de parcelas;
- percentual de desconto fora do intervalo permitido;
- total líquido incompatível com os componentes conhecidos da compra;
- campos obrigatórios ausentes.

Advertências não comprovadamente erradas podem ser mostradas sem bloquear a gravação, desde que o dado permaneça claramente marcado como pendente de revisão.

### Ambiente padrão de testes

A partir desta etapa:

- testes funcionais e visuais serão feitos no aparelho Android físico;
- o computador continuará executando apenas build/testes automatizados necessários;
- scripts padrão não devem iniciar Emulator;
- o Emulator só será utilizado quando solicitado explicitamente;
- comandos de atualização devem preservar os dados do celular com `adb install -r`;
- nenhuma automação pode alterar rotação, brilho, acessibilidade ou outras configurações do aparelho.

Script padrão:

```powershell
.\scripts\atualizar-app-celular.ps1
```

## Próxima etapa imediata — estabilização no celular

Antes de adicionar novas grandes funcionalidades:

1. Instalar a versão mais recente no aparelho físico.
2. Validar:
   - Configurações > Taxonomia;
   - Segmento do estabelecimento;
   - vinculação hierárquica;
   - Central de Revisão;
   - cadastro de Produto Mestre;
   - lançamento manual;
   - importação/exportação TXT;
   - alerta de revisão;
   - dashboard e filtros.
3. Registrar e corrigir problemas encontrados.
4. Revalidar no emulador e no aparelho físico.

A restauração de backup deve ser validada primeiro no emulador antes de ser testada no aparelho principal.

## Fase 2 — leitura por foto / documento

Este é o principal gargalo funcional atual.

### Objetivo

Interpretar corretamente:

- cupons fotografados;
- screenshots;
- PDFs;
- recibos;
- pedidos/delivery;
- documentos sem QR Code;
- forma(s) de pagamento;
- descontos por item e total;
- parcelamento, quando informado no documento.

### Arquitetura planejada

```
Imagem / PDF
      ↓
QR ou chave fiscal?
  ├─ sim → fluxo NFC-e
  └─ não
       ↓
    OCR local
       ↓
resultado suficiente?
  ├─ sim → revisão
  └─ não
       ↓
fallback de IA multimodal
       ↓
JSON estruturado
       ↓
validações locais
       ↓
revisão obrigatória
       ↓
salvar
```

A IA nunca deve salvar dados diretamente sem revisão do usuário.

### IA

Direção atual: avaliar Gemini via Firebase AI Logic como fallback, evitando armazenar chave secreta de API no aplicativo do usuário.

Casos de uso prioritários:

- itens não reconstruídos pelo OCR;
- comprovantes de delivery (ex.: iFood);
- recibos sem QR Code;
- documentos com layout irregular.

## Fase 3 — Modo Compras

Criar uma sessão de compras persistente para uso durante compras presenciais.

### Requisitos

- carrinho persistente;
- total sempre visível;
- adicionar produto sem perder o total atual;
- calculadora/comparador independente;
- quantidade por item;
- estabelecimento atual;
- código de barras;
- leitura de etiqueta de preço;
- histórico recente do Produto Mestre;
- comparação por unidade/kg/litro;
- possibilidade de finalizar manualmente;
- conciliação posterior com NFC-e.

A sessão de compras não deve entrar no Histórico financeiro definitivo até ser finalizada ou conciliada, evitando gasto duplicado.

## Fase 4 — indicadores e análises

Conforme a base real crescer:

- gasto total mensal;
- comparação com mês anterior;
- número de compras;
- ticket médio;
- estabelecimentos onde mais gastou;
- estabelecimentos mais frequentes;
- categorias que mais pesam;
- segmentos que mais pesam;
- Produtos Mestres mais comprados;
- Produtos Mestres com maior gasto;
- maior/menor preço de um Produto Mestre;
- evolução de preços;
- comparação por kg/litro/unidade;
- economia obtida no Modo Compras;
- gastos por PIX, Dinheiro, Débito e Crédito;
- participação do crédito no consumo;
- compras à vista x parceladas;
- quantidade média de parcelas;
- valor comprometido em parcelas futuras, quando os dados permitirem;
- desconto total obtido;
- desconto médio por compra;
- produtos/categorias/estabelecimentos com maior desconto;
- preço bruto x preço líquido efetivo.

## Fase 5 — CNPJ, CNAE e segmento

Objetivo:

```
CNPJ novo
↓
consulta cadastral
↓
CNAE principal/secundários
↓
sugestão de Segmento
↓
confirmação do usuário
↓
aprendizado no Estabelecimento Mestre
```

O banco já reserva informação para CNAE e origem da classificação.

A fonte externa ainda deve ser definida antes da integração. A classificação automática será uma sugestão; o usuário continua podendo confirmar ou alterar.

## Fase 6 — exportação de dados

### Exportação XLSX

Finalidade: análise e consulta no Excel.

Não substitui o backup restaurável.

Estrutura prevista:

- Resumo;
- Compras;
- Itens;
- Produtos;
- Estabelecimentos;
- Vínculos;
- Taxonomia;
- Indicadores.

## Fase 7 — automações futuras

- preenchimento por voz;
- comandos rápidos;
- integração com assistentes do smartphone;
- integração com assistentes de IA;
- sugestões mais avançadas de classificação;
- atualização controlada de taxonomias compartilhadas.

## Pendências importantes

- A leitura OCR local ainda falha em alguns documentos, principalmente na reconstrução dos itens.
- A integração de IA ainda não foi implementada.
- O Modo Compras ainda não foi implementado.
- A exportação XLSX ainda não foi implementada.
- CNPJ → CNAE → Segmento ainda não possui fonte/API escolhida.
- Forma de pagamento, parcelamento, descontos, edição cadastral completa da compra/NF e a camada de validação financeira impeditiva ainda não foram persistidos no banco; o modelo está documentado para entrar junto da evolução da captura/revisão de documentos.
- Comparações históricas precisam de tempo de uso real para ganhar relevância.
- Migrações futuras devem preservar banco, histórico, aprendizado e taxonomia do usuário.

## Ordem oficial de execução

```
1. Instalar e validar a versão atual no celular
2. Corrigir problemas encontrados
3. Estabilizar taxonomia e revisão
4. Melhorar OCR local e estruturar captura de descontos, forma de pagamento e parcelamento
5. Implementar fallback de IA para fotos/recibos, incluindo leitura de descontos e pagamentos quando disponível
6. Persistir descontos, formas de pagamento e parcelamento com validação financeira impeditiva
7. Criar detalhe/edição completa da compra/NF no Histórico, com destaque e navegação para erros
8. Analisar descontos e formas de pagamento no dashboard
9. Criar Modo Compras
10. Adicionar código de barras e leitura de etiqueta
11. Criar comparação por kg/litro/unidade
12. Refinar indicadores e dashboard
13. Validar restauração de backup
14. Implementar exportação XLSX
15. Definir e integrar CNPJ → CNAE → Segmento
16. Voz e assistentes/IA
```

## Regra para manutenção deste roadmap

Ao concluir uma etapa importante:

1. atualizar a seção **Estado atual**;
2. mover a função concluída para **Concluído / funcional**;
3. atualizar a **Ordem oficial de execução**, quando necessário;
4. registrar qualquer decisão que altere arquitetura, dados, segurança ou experiência do usuário.

Este arquivo deve permanecer como a referência de continuidade do projeto.
