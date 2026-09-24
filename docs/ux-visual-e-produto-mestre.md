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
