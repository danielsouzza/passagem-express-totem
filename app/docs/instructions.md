# Projeto: Aplicativo Android para Totem de Autoatendimento

## Contexto Geral

Estou desenvolvendo uma versão Android de um sistema web existente, adaptada para uso em **totens de autoatendimento**. O app deve replicar o fluxo e as funcionalidades do sistema web, porém com uma UX otimizada para interação por toque em telas grandes, sem mouse ou teclado físico.

---

## 📁 Recursos Disponíveis

### 1. Design de Referência

Acesse e leia o arquivo de design disponível na seguinte URL:

```
https://api.anthropic.com/v1/design/h/UB41zNdy6pnUTsMPB-UOxg?open_file=Ferry+Kiosk.html
```

Utilize-o como **referência visual e de fluxo de telas**. Respeite:

- Paleta de cores
- Tipografia
- Estrutura de navegação entre telas
- Hierarquia visual dos componentes
- Adaptações de UX já propostas para o contexto de totem

### 2. Projeto Web de Referência

O projeto web está disponível localmente em:

```
~/Repositories/passagem-express/
```

Antes de escrever qualquer linha de código, **leia e analise o projeto web** para entender:

- Fluxo completo de navegação
- Regras de negócio implementadas
- Modelos de dados utilizados
- Integrações com APIs (endpoints, payloads, autenticação)
- Estados de UI (loading, erro, sucesso, vazio)

---

## 🎯 Adaptações para Totem de Autoatendimento

O app **não é uma cópia fiel** do web. Adapte os seguintes aspectos:

- **Botões grandes** com área de toque mínima de 64dp
- **Tipografia ampliada** — fontes maiores para leitura à distância
- **Teclado virtual customizado** em Jetpack Compose para campos de input (não depender do teclado nativo do sistema)
- **Fluxo simplificado e direto** — remover steps desnecessários, priorizar ações principais
- **Feedback visual e sonoro claro** em cada interação
- **Timeout de inatividade** — após X segundos sem interação, retornar à tela inicial
- **Sem gestos complexos** — tudo acessível por tap simples
- **Layout otimizado** para a orientação definida (landscape ou portrait)

---

## 🏗️ Arquitetura e Padrões Obrigatórios

### Arquitetura: Clean Architecture + MVVM

Organize o projeto nas seguintes camadas:

```
app/
├── data/
│   ├── remote/          # Retrofit interfaces, DTOs, interceptors
│   ├── local/           # Room DAOs, Entities (se necessário)
│   └── repository/      # Implementações dos repositórios
├── domain/
│   ├── model/           # Modelos de domínio (entidades puras)
│   ├── repository/      # Interfaces dos repositórios
│   └── usecase/         # Um arquivo por caso de uso
├── presentation/
│   ├── ui/
│   │   ├── screens/     # Um pacote por tela
│   │   │   └── [tela]/
│   │   │       ├── [Tela]Screen.kt       # Composable raiz da tela
│   │   │       ├── [Tela]ViewModel.kt    # ViewModel da tela
│   │   │       └── [Tela]UiState.kt      # Data class de estado
│   │   └── components/  # Componentes reutilizáveis globais
│   └── navigation/      # NavGraph, rotas, argumentos
├── di/                  # Módulos Hilt
└── core/
    ├── constants/        # Constantes globais
    ├── extensions/       # Extension functions
    └── utils/            # Utilitários gerais
```

---

### Regras de Código Obrigatórias

#### MVVM

- Cada tela tem seu próprio `ViewModel` e `UiState`
- `UiState` é uma `data class` com todos os estados da tela (`isLoading`, `error`, `data`, etc.)
- ViewModels **nunca** importam nada de `androidx.compose` ou `android.view`
- Comunicação ViewModel → UI via `StateFlow` ou `SharedFlow`

#### Jetpack Compose

- **Tudo componentizado** — nenhum Composable com mais de ~80 linhas deve existir sem ser extraído
- Componentes reutilizáveis ficam em `ui/components/`
- Componentes de tela ficam em `ui/screens/[tela]/`
- Parâmetros de estilo (cores, tamanhos) sempre via `MaterialTheme` ou `LocalComposition` — **nunca hardcoded**
- Preview obrigatório em todos os componentes com `@Preview`

#### Clean Code

- Zero hardcode de strings visíveis ao usuário — tudo em `strings.xml`
- Zero hardcode de cores, tamanhos ou espaçamentos — tudo via `theme/` ou `dimens`
- Zero lógica de negócio em Composables
- Nenhuma duplicação de código — extrair funções/componentes sempre que algo se repetir
- Nomes descritivos e em inglês para arquivos, classes, funções e variáveis

#### Injeção de Dependência

- Usar **Hilt** em toda a aplicação
- Um módulo Hilt por camada: `NetworkModule`, `RepositoryModule`, `UseCaseModule`

#### Networking

- **Retrofit** para chamadas HTTP
- **OkHttp** com interceptor de logging (apenas em `debug`)
- Tratar todos os estados: sucesso, erro de rede, erro de servidor, timeout
- Mapear DTOs para modelos de domínio nos repositórios — a camada de domínio nunca conhece DTOs

---

## 📦 Stack Tecnológica

| Biblioteca | Finalidade |
|---|---|
| Kotlin (última estável) | Linguagem principal |
| Jetpack Compose BOM (mais recente) | UI declarativa |
| Hilt | Injeção de dependência |
| Retrofit + OkHttp | Networking |
| Kotlin Coroutines + Flow | Assincronicidade |
| Navigation Compose | Navegação entre telas |
| ViewModel + StateFlow | MVVM |
| Room | Persistência local (se aplicável) |
| DataStore | Preferências / sessão leve |
| Coil | Carregamento de imagens |

---

## ✅ Checklist Obrigatório Antes de Gerar Código

Antes de escrever a primeira linha, execute estas etapas **na ordem**:

1. Acesse o design em `https://api.anthropic.com/v1/design/h/UB41zNdy6pnUTsMPB-UOxg?open_file=Ferry+Kiosk.html` e mapeie todas as telas e componentes presentes

2. Leia o projeto web em `~/Repositories/passagem-express/` e mapeie:
    - Todas as rotas/telas existentes
    - Todos os endpoints consumidos
    - Todos os modelos de dados
    - O fluxo completo do usuário

3. Liste as telas que serão criadas no app Android com base nos dois recursos acima

4. Defina as adaptações de UX de cada tela para o contexto de totem

5. Confirme a estrutura de pastas do projeto antes de começar

Só então comece a implementação, **módulo por módulo**, na seguinte ordem:

1. Configuração do projeto (dependências, Hilt, tema, navegação base)
2. Camada de dados (DTOs, Retrofit, repositórios)
3. Camada de domínio (modelos, use cases)
4. Telas na ordem do fluxo do usuário

---

## 🚫 Proibições Absolutas

- Nenhuma lógica de negócio em Composables
- Nenhuma chamada de API direta em ViewModels (use Use Cases)
- Nenhum `@Composable` com mais de ~80 linhas sem extração de componente
- Nenhum valor de cor, tamanho ou string hardcoded no código
- Nenhuma duplicação de código — sempre extrair e reutilizar
- Não usar `GlobalScope` — sempre `viewModelScope` ou escopo apropriado

---

## 📝 Formato de Entrega Esperado

Para cada tela ou módulo entregue:

1. Estrutura de arquivos criados
2. Código completo de cada arquivo
3. Breve explicação das decisões arquiteturais tomadas, se não forem óbvias