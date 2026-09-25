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
- documentos sem QR Code.

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
- economia obtida no Modo Compras.

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
- Comparações históricas precisam de tempo de uso real para ganhar relevância.
- Migrações futuras devem preservar banco, histórico, aprendizado e taxonomia do usuário.

## Ordem oficial de execução

```
1. Instalar e validar a versão atual no celular
2. Corrigir problemas encontrados
3. Estabilizar taxonomia e revisão
4. Melhorar OCR local
5. Implementar fallback de IA para fotos/recibos
6. Criar Modo Compras
7. Adicionar código de barras e leitura de etiqueta
8. Criar comparação por kg/litro/unidade
9. Refinar indicadores e dashboard
10. Validar restauração de backup
11. Implementar exportação XLSX
12. Definir e integrar CNPJ → CNAE → Segmento
13. Voz e assistentes/IA
```

## Regra para manutenção deste roadmap

Ao concluir uma etapa importante:

1. atualizar a seção **Estado atual**;
2. mover a função concluída para **Concluído / funcional**;
3. atualizar a **Ordem oficial de execução**, quando necessário;
4. registrar qualquer decisão que altere arquitetura, dados, segurança ou experiência do usuário.

Este arquivo deve permanecer como a referência de continuidade do projeto.
