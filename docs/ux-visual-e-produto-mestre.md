# Diretrizes de UX, tema e Produto Mestre

## Produto Mestre

O Produto Mestre representa o **produto raiz que o usuário deseja acompanhar financeiramente**, e não uma marca, fabricante, loja ou descrição fiscal específica.

Exemplos:

- `Macarrão Renata Espaguete 500 g` → Produto Mestre: `Macarrão`
- `Macarrão Liane Parafuso 500 g` → Produto Mestre: `Macarrão`
- `Nissin Miojo Galinha 85 g` → Produto Mestre: `Macarrão instantâneo`

Descrições comerciais, marcas, códigos internos de lojas e variações fiscais permanecem nos vínculos/aliases aprendidos. O histórico financeiro é consolidado pelo Produto Mestre.

## Navegação principal

As áreas principais são:

1. Início
2. Histórico
3. Produtos
4. Perfil

O botão central `+` é uma ação e **não é uma página**. Ele abre o menu rápido para NFC-e, OCR/imagem/PDF e lançamento manual.

As quatro áreas principais aceitam toque na barra inferior e gesto horizontal de arrastar. Telas de tarefa, como lançamento manual, QR/NFC-e, OCR, backup e configurações, não participam do pager para evitar saída acidental durante preenchimento ou leitura.

## Movimento e animações

As animações devem ser perceptíveis, rápidas e funcionais:

- balões do `+`: aproximadamente 150–200 ms, com escala, deslocamento e fade;
- transições de tarefas: aproximadamente 200–300 ms;
- estados de leitura e revisão: animações curtas de entrada/saída;
- animações não devem atrasar ações do usuário.

## Sistema visual

O app oferece:

- modo `Seguir o sistema`;
- modo `Claro`;
- modo `Escuro`;
- paletas Violeta, Oceano, Esmeralda, Pôr do sol e Grafite;
- opção de usar degradê ou cor sólida;
- escala de fonte configurável.

O degradê é usado em cabeçalhos, botão `+`, destaques e elementos de identidade. Formulários e áreas extensas de leitura permanecem em superfícies neutras para preservar legibilidade.

## Segurança do dispositivo

Os scripts de atualização e instalação do projeto não devem alterar configurações do Android, incluindo rotação automática, brilho, escala, acessibilidade ou preferências do sistema.

Para abrir o app após instalar, usar diretamente a Activity:

```powershell
adb -s <serial> shell am force-stop br.com.leitorcuponsfinancas
adb -s <serial> shell am start -n br.com.leitorcuponsfinancas/.MainActivity
```

Não usar `monkey` apenas para iniciar o aplicativo.


## Padrão de botões e ações

As ações do aplicativo devem ter hierarquia visual consistente.

### Ação primária

Usar botão preenchido e com a cor `MaterialTheme.colorScheme.primary`, acompanhando automaticamente a paleta escolhida pelo usuário.

Exemplos:

- Confirmar;
- Criar;
- Criar e vincular;
- Vincular;
- Usar sugestão;
- OK;
- Prosseguir;
- Salvar.

Em diálogos e formulários, deve existir preferencialmente **uma ação primária final**. Ela só fica habilitada quando os campos mínimos necessários estiverem válidos.

### Ação neutra

Usar botão de contorno ou texto com `onSurfaceVariant`, sem competir visualmente com a ação principal.

Exemplos:

- Cancelar;
- Fechar;
- Voltar;
- Trocar segmento;
- Voltar um nível;
- escolher uma opção intermediária da hierarquia.

### Ação destrutiva

Excluir/apagar deve usar semântica de erro e exigir confirmação quando houver risco de perda de dados. Não reutilizar a aparência da ação positiva.

### Sugestão inteligente

Quando o aplicativo apresenta uma correspondência existente, por exemplo:

```
Possível correspondência
Detergente • 92% de compatibilidade

[ Usar Detergente ]
```

tocar em **Usar Detergente** significa aceitar a sugestão e deve vincular o item diretamente ao Produto Mestre sugerido. Não abrir novamente toda a navegação da taxonomia nem exigir recriação do Produto Mestre.

Se a sugestão já possuir classificação taxonômica compatível com o segmento do estabelecimento, essa classificação deve ser reutilizada automaticamente.

### Vinculação manual

Quando o usuário optar por vinculação manual:

1. navegar pela hierarquia;
2. selecionar um Produto Mestre existente **ou** entrar no modo de criação;
3. opções intermediárias permanecem visualmente neutras;
4. o botão final `Vincular`, `Confirmar vínculo` ou `Criar e vincular` fica em destaque e usa a cor do tema;
5. `Cancelar` permanece neutro.

Selecionar um Produto Mestre na lista não deve salvar imediatamente; a seleção é mostrada e a ação final confirma a operação.


## Padrão dos campos de texto

Todos os campos de entrada do aplicativo devem ocupar **uma única linha**.

Comportamento esperado:

- o texto não aumenta a altura do campo;
- conforme a digitação avança, o conteúdo anterior permanece para trás e o campo acompanha o cursor horizontalmente;
- o cursor permanece visível na posição atual;
- campos textuais digitados pelo usuário iniciam com a primeira letra em maiúscula;
- o restante do texto é preservado exatamente como o usuário digitou.

Exemplo:

```
detergente neutro
↓
Detergente neutro
```

A capitalização vale para textos semânticos, como:

- nome de produto;
- estabelecimento;
- categoria;
- subcategoria;
- observação;
- nome do usuário;
- pesquisa textual;
- descrição corrigida.

### Exceções de integridade

Campos técnicos não devem ter seu conteúdo alterado automaticamente, porque qualquer modificação pode invalidar o dado:

- URL;
- chave de acesso;
- CNPJ/CPF;
- códigos;
- números de documento;
- datas;
- valores;
- quantidades.

Esses campos continuam em uma única linha, mas preservam exatamente os caracteres válidos informados/importados.

Unidades padronizadas, como `UN`, `KG` e `L`, continuam sendo convertidas para maiúsculas integralmente.
