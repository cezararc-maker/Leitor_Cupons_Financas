# Arquitetura inicial

## Objetivo

Aplicativo Android local-first para capturar NFC-e do Mato Grosso do Sul, revisar os dados e registrar compras e produtos para controle financeiro pessoal.

## Módulos funcionais

### 1. Captura
- Leitura de QR Code pela câmera.
- Importação futura de imagem/PDF.
- Entrada manual da chave de acesso.

### 2. NFC-e MS
- Identificação de URL da consulta pública.
- Consulta da página pública.
- Extração de emitente, CNPJ, endereço, data, itens e totais.
- Parser isolado para facilitar testes e futura inclusão de outros estados.

### 3. Cadastro manual
Produtos podem ser incluídos sem vínculo com NFC-e.

Campos iniciais:
- descrição;
- nome normalizado;
- setor;
- categoria;
- subcategoria;
- unidade;
- observações;
- ativo/inativo.

### 4. Compras
Cada compra guarda:
- origem (NFC-e ou manual);
- estabelecimento;
- CNPJ/CPF quando disponível;
- endereço;
- data;
- total;
- chave/URL da NFC-e quando aplicável;
- itens vinculados.

### 5. Classificação
A classificação financeira será independente da descrição fiscal do produto. Correções do usuário poderão alimentar regras locais de classificação.

### 6. Persistência
Banco local Room/SQLite. Nenhum servidor será obrigatório no MVP.

## Tecnologias propostas

- Kotlin;
- Jetpack Compose;
- Room/SQLite;
- CameraX;
- ML Kit Barcode Scanning para QR Code;
- OkHttp para HTTP;
- parser HTML isolado;
- testes unitários + instrumentados.

## Princípios

- GitHub como fonte oficial;
- local-first;
- sem dependência de API paga;
- dados financeiros permanecem no aparelho no MVP;
- parser da SEFAZ desacoplado para permitir suporte futuro a outras UFs.
