# Passagens Express — Progresso

> Snapshot do estado em **2026-05-15** após sessão de refatoração visual do `:feature:passenger` + extração de keypads pro design system. Passenger virou um form único por passageiro (sem tabs, sem bloco "Dados para Contato"): o contato é **derivado automaticamente do passageiro #1** (nome + telefone; email vazio). Nome agora usa keypad alfanumérico Compose custom. `:core:designsystem` ganhou `TotemNumericKeypad` + `TotemAlphaKeypad` (QWERTY PT-BR com shift + acentos + ?123), `TotemInputField` (outlined com label flutuante + caret piscando quando focused) e slot `stickyBottom` no `TotemScreenScaffold` (keypad fixo no rodapé, full-width). Fix de bug crítico em navegação: `passengerRoute`/`paymentRoute` colavam JSON cru na URL — agora re-URL-encodam. AndroidManifest aceita cleartext HTTP (`android:usesCleartextTraffic="true"`) pra dev local. O fluxo Idle → City → Date → Trip → Room → Passenger → Payment(PIX) está completo; falta o cartão e a tela de confirmação.

---

## 1. O que é

Versão Android do sistema web `passagem-express` (Vue 3 + Framework7), adaptada para **totens de autoatendimento** que vendem passagens de barco/ferry. Consome a mesma API REST do web (`https://app.techrios.online/`).

**Hardware-alvo:** 10.1" multitouch, **portrait** (revisado de landscape), sem teclado físico. Maquininha de cartão é dispositivo separado posicionado ao lado do totem.

**Referências externas:**
- Projeto web local: `~/Repositories/passagem-express/`
- Protótipo de design (Claude Design, Ferry Kiosk): `app/docs/design/passagens-express/`
- Instruções originais do cliente: `app/docs/instructions.md`

---

## 2. Decisões arquiteturais

| Decisão | Escolha | Motivo |
|---|---|---|
| Arquitetura | Clean Architecture + MVVM | Separação clara, testabilidade |
| Modularização | Multi-module Gradle por feature | Build incremental + escala |
| Escopo MVP | Paridade total com web | Cliente quer assigned-seat + N passageiros + recuperação de pedido |
| Provisionamento | APK único + setup wizard na 1ª boot | Simplifica deploy; um build para todos os totens |
| Subdomínio | Opcional no wizard | Suporta deploys single-tenant |
| Pagamento cartão | Delegado a maquininha física, app só faz polling | Hardware separado |
| Pagamento PIX | App gera QR + faz polling em `/api/pedidos/{id}/status` | Padrão BR |
| Idioma | PT-BR + EN-US toggle | Solicitado pelo cliente |
| Orientação | Portrait (revisada) | Decisão do usuário em 2026-05-06 |

---

## 3. Stack

```
Kotlin 2.2.10                    Compose BOM 2026.02.01
AGP 9.2.1                         Material 3
Hilt 2.59.2 (KSP)                 Navigation Compose 2.8.5
Retrofit 2.11 + OkHttp 4.12       DataStore Preferences 1.1.1
kotlinx-serialization 1.7.3       Lifecycle 2.8.7
Coroutines 1.9                    Splashscreen 1.0.1
KSP 2.2.10-2.0.2                  ZXing core 3.5.3 (QR gen)
```

Versões em `gradle/libs.versions.toml`.

---

## 4. Estrutura de módulos

```
:app                      MainActivity, TotemApplication, TotemNavHost, TotemRootViewModel,
                          TotemAppChrome (provê LocalTotemStatusBar p/ as flow screens)
:core:designsystem        Theme (TotemTheme + TotemPalette + TotemDimens + TotemMotion + TotemShapes + TotemTypography)
                          Componentes: TotemPrimaryButton, TotemSecondaryButton, TotemCard,
                          TotemScreenScaffold (com slot `stickyBottom` p/ keypad fixo no rodapé),
                          TotemLoading, TotemErrorState,
                          TotemStatusBar + LocalTotemStatusBar (chrome do totem),
                          TotemInputField (outlined, label flutuante, caret piscando p/ focused),
                          TotemNumericKeypad + TotemAlphaKeypad (QWERTY PT-BR c/ shift + acentos +
                          ?123 toggle, teclas com press animation via scale + ripple)
:core:common              AppResult/AppError, AppDispatcher (Kotlin JVM puro)
:core:domain              Modelos puros + interfaces de repositório + use cases (Kotlin JVM puro)
:core:datastore           TotemConfigDataStore (subdomain, portoId+portoSlug+portoNome,
                          municipioCodigo+municipioNome, lang, setupComplete)
:core:network             Retrofit + OkHttp + AuthInterceptor (X-API-KEY + subdomain)
                          BuildConfig: API_BASE_URL, API_TOKEN, ENABLE_HTTP_LOGGING
:core:data                DTOs + mappers + RepositoryImpls + ApiModule + RepositoryModule
:feature:setup            Wizard 3 etapas (subdomínio opcional → porto com busca → idioma)
:feature:idle             Tela inicial touch-to-start + secret long-press canto sup-esq abre setup
:feature:city             Tela única de destino (porto já fixa origem)
:feature:date             LazyRow de pílulas com 14 dias a partir de hoje
:feature:trip             Lista trechos do dia + date switcher inline + fallback "próxima viagem"
:feature:room             Ramifica entre poltronas (grid linhas×colunas), camarotes (cards) e livre (contadores).
                          Reserva/cancela cômodo individual via API, conta seleção por tipo localmente,
                          chama `iniciarVenda` ao avançar.
:feature:passenger        Formulário N passageiros (1 por cômodo) — exibe **um por vez**,
                          navegando via `← Anterior / Próximo →` (escondidos quando size==1).
                          Header per-form: "PASSAGEIRO X" + "Dados para emissão do bilhete." +
                          botão "× remover" (visual no-op por enquanto, aparece só com size>1).
                          Campos: select de Tipo de documento (CPF/RG via `DropdownMenu`),
                          Nº doc, Nome (keypad alfanumérico), Celular, Data de nascimento.
                          Todos usam `TotemInputField` do designsystem (label flutuante + caret
                          piscando quando focused). Keypads renderizam no `stickyBottom` do
                          scaffold (full-width, colados no footer). Busca por documento
                          (`BuscarPassageiroUseCase(tipo,doc)`) dispara no Concluído do keypad.
                          **Contato derivado**: `forms[0]` vira o contato no submit
                          (`deriveContato` em `PassengerViewModel`); email vai vazio (Payment
                          mapeia blank → null). Recebe `trechoJson` (opaco) + `inicioVendaJson`
                          via nav. Emite `PassengerCompleted { trechoId, passageiros, contato, rawTrechoArg }`.
:feature:payment          Pagamento PIX (cartão ainda pendente). Init chama `CriarPedidoUseCase`
                          montando `NovoPedido` a partir do trecho + passageiros + contato
                          (contato derivado do passageiro #1; document/tipo/nascimento herdados
                          também do #1 via `passageiros.first().toPassageiroDomain()`;
                          `desconto_id` = `trecho.desconto?.id` por item; `ItemPedido.isContact`
                          = `index == 0`). Form contato-PIX (Nome IME + CPF NumericKeypad)
                          pré-preenchido, editável. "Gerar PIX" → `GerarPagamentoPixUseCase(pedidoId,cpf,nome)`
                          retorna `pix_copia_cola`. QR gerado localmente via ZXing
                          (`feature.payment.ui.QrCodeGenerator`). Polling 10s via
                          `ObterStatusPedidoUseCase` (compara `PedidoStatus.Pago`). Timer 30min.
                          Emite `PaymentApproved(pedidoId)` na aprovação.
```

---

## 5. O que está implementado (✅) e o que falta (⏳)

### Camada de dados — ✅ completa

DTOs e mappers para todos os endpoints listados:
- `ViagemApi` — portos, filtros, trechos-viagem, get-passageiro
- `ComodoApi` — poltronas, camarotes, livres-por-tipo, reservar/deletar/iniciar-venda
- `PedidoApi` — criar, ultimo-aberto, gerar-passagens, status
- `PagamentoApi` — pix, credito

Implementações:
- `ViagemRepositoryImpl` — inclui fallback "próxima viagem disponível" quando busca retorna vazio. Métodos: `buscarPortos()`, `buscarMunicipiosOrigem(portoSlug)`, `buscarMunicipiosDestino(portoSlug, municipioOrigemCodigo)`, `buscarViagens(filtros)`, `buscarPassageiro(tipo, doc)` (endpoint exige `tipo`=id de `TipoDocumento` + `doc`=dígitos puros)
- `ComodoRepositoryImpl`, `PedidoRepositoryImpl`, `PagamentoRepositoryImpl`

Helpers e serializers custom (em `core/data/remote/parse/`):
- `safeApiCall` mapeia exceções HTTP/rede → `AppError`
- `callEnvelope`/`callEnvelopeNullable` desembrulham `ApiEnvelope { success, data, message }`
- `parseMoney("R$ 1.234,56")`, `parseLocalDate`, `parseLocalTime`, `parseLocalDateTime`, `parseTempoViagemMinutos`
- `IntBooleanSerializer` — aceita `0/1` ou `true/false` (backend envia ints em flags como `poltrona_livre`, `is_ocupado`).
- `PhpAssocMapSerializer` — aceita `{}` ou `[]` vazio como mapa. Aplicado em `ComodosByTipoDto` (resposta de `/api/comodos/poltronas` e `/api/comodos/camarotes` vem como `"data": []` quando vazia, pois PHP serializa assoc-array vazio como array).

### Telas implementadas — ✅

- **Setup wizard** (3 etapas):
  1. Subdomínio (opcional, OutlinedTextField com IME nativo — operador faz setup com USB keyboard)
  2. Porto de operação (LazyVerticalGrid com **busca live** por nome/município, empty state)
  3. Idioma padrão (PT-BR / EN-US)
- **Idle** — gradient azul fullscreen, brand mark, CTA pulsante PT/EN, mostra porto configurado no rodapé. Long-press no canto superior esquerdo (até 120dp²) reabre o setup.
- **City** — tela única de **destino**. O porto configurado no setup já fixa a cidade de embarque, então não pergunta origem. Carrega destinos direto via `/api/filtros?porto_id=X` (que retorna `municipiosDestino` quando há porto). `LazyVerticalGrid` com hero gradient + monogram, busca live + empty/error/retry states.
- **Date** — `LazyRow` de pílulas (DOW/dia/mês), 14 dias a partir de hoje, badge "Hoje"/"Amanhã". Estado inteiramente local, sem chamada de API.
- **Trip** — header com origem/destino, **date switcher inline** (← prev / data atual / next →), `LazyColumn` de cards (HH:mm origem → ferry icon → HH:mm destino → preço), tag "Livre escolha" / "Poltrona marcada". Fallback "próxima viagem disponível" puxa do `proximaViagem` quando a data atual não tem trechos.
- **Room** — recebe o `Trecho` via JSON URL-encoded (`TrechoNavPayload`). Detecta o modo a partir de `tiposComodos` + `poltronaLivre`:
  - **Poltronas** (id=1, não-livre) → grid `linhas × colunas` de chips coloridos (verde livre / azul selecionado / cinza ocupado), cada toque chama `reservarComodo`/`deletarReserva` via API.
  - **Camarotes** (id=4) → `LazyVerticalGrid` 3 colunas com cards individuais.
  - **Livres** (poltrona livre OU outros tipos id≠1,4) → cards-contador por tipo, seleção local incremental.
  - Modos com poltrona/camarote também mostram cards-contador para "outros tipos" se houver (ex: rede + camarote na mesma viagem).
  - Lista de "Cômodos selecionados" abaixo, com remoção por X e steppers ± para quantidade por tipo.
  - Banner de erro transitório (ex: "vaga já reservada") com X de dismiss.
  - Botão "Avançar" chama `iniciarVenda(tiposComodoEscolhidos, comodosAssentosEscolhidos)` e emite `InicioVenda` para a próxima tela.
  - "Voltar" envia `onAbandonScreen` (deleta reservas in-flight, fire-and-forget).
- **Passenger** — recebe `InicioVenda` via `InicioVendaNavPayload` JSON URL-encoded. Constrói 1 `PassageiroForm` por cômodo (`inicioVenda.comodos.size` formulários). **Refatorado em 2026-05-15**: sem tabs, sem checkbox `isContact`, sem bloco "Dados para Contato" — simplificado para tela de form único navegando entre passageiros.
  - **Layout** (todos `TotemInputField` outlined, label flutuante, caret piscando quando keypad daquele field está aberto):
    - Header do form: `PASSAGEIRO X` (uppercase, letter-spacing 1sp) + subtítulo "Dados para emissão do bilhete." + botão `× remover` à direita (no-op visual; aparece só quando `forms.size > 1`).
    - Linha 1 (2 colunas): `DocTypeSelect` (DropdownMenu CPF/RG, label flutuante) | `Nº do documento` (keypad numérico).
    - Linha 2: `Nome completo` (keypad alfanumérico, full-width).
    - Linha 3 (2 colunas): `Celular` | `Data de nascimento`.
    - `← Anterior / Próximo →` abaixo, **escondido quando `forms.size == 1`**.
  - **Keypads** (`:core:designsystem.TotemNumericKeypad` e `TotemAlphaKeypad`): renderizados no slot `stickyBottom` do `TotemScreenScaffold` — full-width, colados no footer, body acima continua scrollável. `TotemAlphaKeypad` tem QWERTY PT-BR com shift, `?123` toggle (símbolos), strip de acentos (á é í ó ú ã õ), space, backspace. Teclas com animação de press (scale 0.94f via spring) + ripple.
  - **Máscaras** em `feature.passenger.format.Masks.kt`: CPF (`###.###.###-##`), telefone (`(##) #####-####` ou `(##) ####-####`), data (`dd/mm/aaaa`). `digitsOnly` extrai dígitos para a API. Aplicadas no `applyKeypadEdit` do VM.
  - **`KeypadField` sealed interface**: `Documento`/`Telefone`/`Nascimento`/`Nome` — VM resolve qual keypad mostrar via `KeypadSlot` na tela.
  - **Busca de passageiro** disparada ao tocar "Concluído" do keypad quando o documento bate o tamanho válido pro tipo (`DocumentoMask.isValid`: 11 dígitos pra CPF, ≥5 pra RG). `BuscarPassageiroUseCase(tipo, doc)` → `ViagemRepository.buscarPassageiro(tipo, doc)` → `/api/filtros/get-passageiro?tipo=<id>&doc=<digitos>`. Auto-preenche `nome`, `telefone`, `dataNascimento` se encontrar.
  - **Validação:** ao "Avançar", valida todos os formulários (documento, nome, telefone, nascimento). Pula automaticamente para o primeiro formulário com erro. Não exige mais "marcar como contato" — passageiro #1 é o contato por convenção.
  - **Contato derivado**: `PassengerViewModel.deriveContato(forms.firstOrNull())` cria `ContatoForm(nome=#1.nome, telefoneDisplay=#1.telefoneDisplay, email="")` no submit. `PaymentViewModel.buildNovoPedido` mapeia email blank → null no JSON.
  - **Saída:** `PassengerCompleted { trechoId, passageiros, contato, rawTrechoArg }`.
- **Payment** (PIX) — recebe `trechoJson` + `saleJson` (`PaymentSalePayload { passageiros, contato }`). Decoda Trecho via cópia local de `PaymentTrechoPayload` (mesmo shape do `TrechoNavPayload` de feature:room — sem dep cruzada). PassageiroForm/ContatoForm são snapshotados em `PaymentPassageiroPayload`/`PaymentContatoPayload` (sem `errors`).
  - **Criação do pedido on init:** `buildNovoPedido` monta `NovoPedido` com:
    - `base = passageiros.first().toPassageiroDomain()` (passageiro #1 é o contato por convenção no totem — sem mais checkbox `isContact`).
    - `contato = base.copy(nome=contatoSnapshot.nome, email=contatoSnapshot.email.takeIf{it.isNotBlank()}, telefone=PhoneMask.digitsOnly(contatoSnapshot.telefoneDisplay))`.
    - `dataHora` no formato `dd/MM/yyyy`, `origem = 3`.
    - **Cada `ItemPedido` carrega `descontoId = trecho.desconto?.id`** (null quando sem desconto, alinhando com `home.vue#populateComodos:431`).
    - **`ItemPedido.isContact = (index == 0)`** — primeiro item do `dataComodos[]` é o contato (a API espera exatamente um item com `isContact=true`).
  - **Resumo da viagem:** route + data + hora + nº de passageiros + subtotal/taxa/total.
  - **Card "Dados do contato" no PIX:** Nome IME + CPF NumericKeypad, pré-preenchido com `contato.nome` + cpf do passageiro #1. Editável; valida nome+sobrenome e CPF de 11 dígitos antes de chamar `gerarPagamentoPix`.
  - **PIX:** botão "Gerar PIX" → `GerarPagamentoPixUseCase(pedidoId, cpf, nome)`. Backend devolve `pix_copia_cola` (BR Code). QR é gerado **localmente** via ZXing 3.5.3 (`feature.payment.ui.QrCodeGenerator.generatePixQrCode`) — não usa `qr_code_base64` (vinha vazio do gateway). Sem botão de copiar (totem não precisa).
  - **Timer 30min** decrementa via coroutine; quando chega a 0 → `PixStage.Expired` (oferece "Gerar novo PIX").
  - **Polling 10s:** `ObterStatusPedidoUseCase`; quando retorna `PedidoStatus.Pago`, set `PaymentStatus.Approved` e emite `PaymentApproved(pedidoId)`. `TotemNavHost` ainda só loga (próximo: `:feature:confirm`).

### Telas pendentes — ⏳ em ordem do fluxo

| # | Tela | Use case principal |
|---|---|---|
| 1 | `:feature:payment` — cartão | Hoje só PIX está implementado. Falta o fluxo de cartão (instrução para a maquininha externa + polling). |
| 2 | `:feature:confirm` | bilhete + auto-reset 30s + imprimir |
| 3 | `:feature:resumeorder` | modal "pedido em aberto encontrado" — verificação no boot |
| ⏳ | "× remover" passageiro | Hoje o botão é no-op visual. Definir semântica (liberar reserva via `deletarReserva` + remover form da lista + voltar pra Room se ficar 0). |

### Outros pendentes

- ⏳ **Long-press secret 5s** — hoje é o default Compose (~500ms). Reescrever com `pointerInput` custom quando virar requisito real.
- ⏳ **Timeout global de inatividade** (60s → reset). Implementar quando tiver o fluxo de compra.
- ~~**Tela de status PT/EN toggle** persistente~~ ✅ — `TotemStatusBar` em `:core:designsystem` (brand + porto + horário tickando por minuto + toggle PT/EN persiste via `SetLanguageUseCase`). Fornecido via `LocalTotemStatusBar` por `TotemAppChrome` (em `:app/ui/chrome/`), aplicado só ao ramo Idle do `TotemApp` — Setup e Idle mantêm chrome próprio. `TotemScreenScaffold` lê o local e renderiza acima do título.
- ⏳ **Stepper component** (indicador de passos na faixa superior das telas de fluxo).
- ⚠ **Navegação com JSON opaco**: `SavedStateHandle.get<String>(ARG)` retorna a string **já decodificada** pelo Compose Navigation. Quando uma feature passa esse arg adiante via `passengerRoute(...)`/`paymentRoute(...)`, o builder de rota **precisa re-URL-encodar** antes de colar no path — caso contrário o NavController não consegue casar a rota e crasha com `IllegalArgumentException: Navigation destination ... cannot be found`. Fix aplicado em `feature/passenger/.../PassengerNavigation.kt` e `feature/payment/.../PaymentNavigation.kt`.

---

## 6. Configuração necessária

### `~/.gradle/gradle.properties` (NÃO no projeto — vaza no Git)

```properties
passagenexpress.apiToken=COLE_O_TOKEN_AQUI
```

Token atual do projeto está em `gradle.properties` do repositório (homologação). Para produção, pegar no `.env` do projeto web.

### `gradle.properties` do projeto

Já contém os flags necessários:
```properties
android.disallowKotlinSourceSets=false
```
(workaround AGP 9 + KSP 2.2.10 — remover quando bumpar Kotlin → 2.3+)

### API base URL

Hardcoded em `core/network/build.gradle.kts`:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://app.techrios.online/\"")
```

Atualmente apontando para **produção**. Para alternar entre prod/homolog, considerar build flavors no futuro.

### Cleartext HTTP (dev)

`AndroidManifest.xml` tem `android:usesCleartextTraffic="true"` para permitir API em HTTP/IP local durante dev. **Antes de release**: mover pra `app/src/debug/AndroidManifest.xml` (manifest merging do AGP) ou usar `network_security_config.xml` restringindo cleartext só ao IP do servidor.

---

## 7. Workarounds conhecidos (AGP 9)

| Problema | Workaround |
|---|---|
| `Cannot add extension with name 'kotlin'` | NÃO aplicar `org.jetbrains.kotlin.android` em módulos Android — AGP 9 traz Kotlin embutido. Usar só `kotlin.compose`/`kotlin.serialization` quando necessário. |
| `kotlinOptions { jvmTarget = ... }` não disponível | Usar só `compileOptions { source/targetCompatibility = JavaVersion.VERSION_11 }` — Kotlin built-in herda. Override explícito via `kotlin { compilerOptions { ... } }`. |
| `Android BaseExtension not found` (Hilt) | Hilt < 2.59 não conhece a API nova do AGP 9. Usar **Hilt 2.59.2+**. |
| `Using kotlin.sourceSets DSL is not allowed with built-in Kotlin` (KSP) | Adicionar `android.disallowKotlinSourceSets=false` em `gradle.properties`. Fix definitivo só em KSP 2.3.1+ que requer Kotlin 2.3+. |
| Versão do KSP mal-pinada | KSP versionamento legado é estrito ao Kotlin (`2.2.10-x.y.z`). Confirmar via Maven Central antes de pinar — não chutar sufixo. |

---

## 8. Backend API — cheat sheet

**Base URL:** `https://app.techrios.online/`
**Header:** `X-API-KEY: <token>` em toda requisição
**Multi-tenant:** parâmetro `subdomain` (query em GET, body em POST) — opcional

| Método | Path | Função |
|---|---|---|
| GET | `/api/filtros/portos` | lista portos |
| GET | `/api/filtros` | filtros → `{ municipiosOrigem, municipiosDestino }`. **Com `porto_id` → preenche `municipiosDestino` direto** (no totem é sempre esse caso). Sem porto + `municipio_origem` → também preenche destinos. |
| GET | `/api/trechos-viagem` | viagens. **`porto` é o slug do porto** (não id). `municipio_origem` é o **código IBGE** do município, e fica null quando há porto. Outros params: `destino` (slug), `data_hora`, `quantia`, `data_irrestrita`. |
| GET | `/api/filtros/get-passageiro` | dados de passageiro por documento. **Query exige `tipo` (id de `TipoDocumento`: CPF=5, RG=1, …) + `doc` (dígitos sem máscara).** |
| GET | `/api/comodos/poltronas` | assigned-seat (poltronas) por trecho/viagem |
| GET | `/api/comodos/camarotes` | assigned-seat (camarotes) |
| GET | `/api/comodos/livres-por-tipo` | free-seating (contagem por tipo) |
| POST | `/api/reservas/comecar-venda` | iniciar venda |
| POST | `/api/reservas/comodo` | reservar cômodo individual |
| DELETE | `/api/reservas/comodo` | cancelar reserva |
| POST | `/api/pedidos` | criar pedido (origem=3 para totem) |
| GET | `/api/pedidos/ultimo-aberto/dados` | recuperar carrinho aberto |
| POST | `/api/pedidos/{id}/gerar-passagens` | confirmar |
| GET | `/api/pedidos/{id}/status` | polling de status |
| POST | `/api/pagamentos/pix` | gerar QR PIX |
| POST | `/api/pagamentos/credito` | iniciar cartão (na maquininha externa) |

Todas envolvem em `{ success: bool, data: T, message: string }`. `parseMoney` para campos `valor`/`taxa_de_embarque` que vêm como string `"R$ 1.234,56"`.

### Peculiaridades do backend (PHP/Laravel)

| Campo | Quirk | Tratamento |
|---|---|---|
| `poltrona_livre`, `is_ocupado` | Vêm como `0`/`1` em vez de boolean | `IntBooleanSerializer` |
| `data` em `/api/comodos/poltronas` e `/api/comodos/camarotes` | Quando vazio, vem `[]` em vez de `{}` (PHP assoc-array vazio) | `PhpAssocMapSerializer` via `ComodosByTipoDto` |
| `formas_pagamento` em `iniciarVenda` | Lista de objetos `{id, codigo, nome}`, não strings | `FormaPagamentoDto` → `FormaPagamento` |
| `/api/filtros?porto_id=X` | O **valor** é o **slug** do porto (apesar do nome `porto_id`) | `ViagemApi.getFiltros(portoSlug: String?)` |
| `/api/trechos-viagem?porto=X` | Slug do porto. `municipio_origem` = código IBGE, fica null quando há porto | `BuscaViagensFiltros(portoSlug, municipioOrigemCodigo)` |
| `/api/filtros/get-passageiro` | Exige `tipo` + `doc` (não `cpf`). E **retorna o passageiro direto no root**, sem envelope `{success,data,message}` | `ViagemApi.getPassageiro(tipo,doc): PassageiroDto?` + `safeApiCall` no repo |
| `desconto_id` em `dataComodos[]` | Web sempre envia explícito (`null` quando trecho sem desconto) | `NetworkJson` com `explicitNulls = true` (default) — não omite nulos |
| `pix_copia_cola` em `/api/pagamentos/pix` | Nome do campo é `pix_copia_cola` (web lê `paymentPending.pix_copia_cola`); `qr_code_base64` vem vazio | `PagamentoPixResponseDto.pixCopiaCola` + QR gerado localmente com ZXing |

Detalhes em `~/Repositories/passagem-express/src/js/services/{Viagem,Pedido,Comodo}Service.js`, com referência ao uso em `pages/home.vue` e `components/Onboarding.vue`.

---

## 9. Modelos de domínio chave

```kotlin
// Localização: core/domain/src/main/kotlin/com/example/passagenexpress/core/domain/model/

Porto(id, slug, nome, municipioCodigo, municipioNome)
Municipio(slug, nome, uf?)
Trecho(id, idViagem, dataEmbarque, horario, tempoViagemMinutos, valor, taxaDeEmbarque,
       embarcacao, poltronaLivre, linhas, colunas, tiposComodos, municipioOrigem,
       municipioDestino, desconto?)
TipoComodo(id, nome)  // id 1 = poltrona; id 4 = camarote; demais = cabines
Comodo(id, numeracao, nome?, linha, coluna, isOcupado, tipoComodidadeId, valor?, quantidade)
ComodoLivre(tipoComodidadeId, quantidade)
Passageiro(nome, cpf, telefone, email?, dataNascimento?, tipoDocumento)
Pedido(id, status, totalPassagens, totalTaxas, total, criadoEm, itens)
ItemPedido(comodoId, tipoComodidadeId, passageiro, valor, taxaEmbarque, descontoId?, ...)
Pagamento.Pix(pedidoId, valor, qrCodeBase64?, copiaECola, expiraEm?)
Pagamento.Cartao(pedidoId, valor, tipo)
FormaPagamento(id, codigo, nome)  // vinda do backend em iniciarVenda
StatusPagamento { Pendente, Aprovado, Recusado, Expirado }
TotemConfig(subdomain, portoId, portoSlug, portoNome, municipioCodigo, municipioNome,
            defaultLanguage, setupComplete)
AppLanguage { PtBr("pt-BR"), EnUs("en-US") }
```

---

## 10. Fluxo do app

```
MainActivity
   └── TotemApp
        └── TotemRootViewModel observa TotemConfig (Eagerly)
             └── emite RootDestination:
                  ├── Loading      → TotemLoading
                  ├── Setup        → NavHost(start=SETUP)
                  │                    └── SetupWizardScreen
                  │                         (subdomain → porto[busca] → idioma → completar)
                  │                         └── on complete → navigate(IDLE)
                  └── Idle         → NavHost(start=IDLE)
                                       └── IdleScreen
                                            ├── tap = navigate(CITY)
                                            └── long-press canto = navigate(SETUP)

           CITY(destino) → DATE(destinoSlug,destinoNome) → TRIP(destinoSlug,destinoNome,date)
                         → ROOM(trechoJson) → PASSENGER(trechoJson,inicioVendaJson)
                         → PAYMENT(trechoJson,passengersJson) → TODO(CONFIRM)
           (origem é implícita do porto configurado; trechoJson trafega opaco até Payment, que decoda)
```

---

## 11. Roadmap sugerido (ordem)

1. ~~**`:feature:city` + `:feature:date` + `:feature:trip`**~~ ✅ — primeiras 3 telas do fluxo de compra prontas. Use cases novos em `core/domain/usecase/`: `BuscarMunicipiosOrigemUseCase`, `BuscarMunicipiosDestinoUseCase(portoSlug, municipioOrigemCodigo)`, `BuscarViagensUseCase`. Rotas carregam só `destinoSlug+destinoNome+date` — origem é implícita do porto configurado.
2. ~~**`:feature:room`**~~ ✅ — ramifica entre poltronas / camarotes / free-seating com base em `Trecho.poltronaLivre` e `Trecho.tiposComodos`. Use cases: `ListarPoltronas/Camarotes/ComodosLivresUseCase`, `ReservarComodoUseCase`, `DeletarReservaUseCase`, `IniciarVendaUseCase`. Trecho passa via `TrechoNavPayload` JSON URL-encoded.
3. ~~**`:feature:passenger`**~~ ✅ — formulário N passageiros (um por vez via Anterior/Próximo, escondido quando size==1). Todos os inputs usam `TotemInputField` outlined + caret piscando; teclados `TotemNumericKeypad`/`TotemAlphaKeypad` (QWERTY PT-BR) no `stickyBottom` do scaffold. Contato derivado de `forms[0]` automaticamente. Máscaras inline (`DocumentoMask`/`PhoneMask`/`BirthdateMask`). Busca de passageiro `BuscarPassageiroUseCase(tipo,doc)`. Payload chega via `InicioVendaNavPayload` + `trechoJson` (opaco pass-through). Emite `PassengerCompleted(trechoId, passageiros, contato, rawTrechoArg)`.
4. ~~**`:feature:payment`** (PIX)~~ ✅ — Init monta `NovoPedido` com contato editável + `desconto_id` por item (`trecho.desconto?.id`) e chama `CriarPedidoUseCase`. Card de PIX tem form Nome+CPF editável (pré-preenchido). "Gerar PIX" → `GerarPagamentoPixUseCase(pedidoId, cpf, nome)` (body `{pedido_id, cpf, nome}`). Backend devolve `pix_copia_cola` → QR gerado **localmente via ZXing 3.5.3** (`generatePixQrCode`). Polling 10s via `ObterStatusPedidoUseCase` → quando `PedidoStatus.Pago`, emite `PaymentApproved`. Timer 30min (`PIX_TIMEOUT_SECONDS`). Sem botão copiar (totem). **Cartão ainda pendente.**
5. **`:feature:confirm`** — bilhete + auto-reset 30s. Integrar impressão (TODO: SDK específico do hardware).
6. **`:feature:resumeorder`** — checa `obterUltimoPedidoAberto` no boot, mostra modal.
7. **Polish:** timeout global, status bar com PT/EN toggle, stepper, animações de transição entre telas.
8. **Testes:** instrumentation tests para o fluxo end-to-end.

### Heurísticas para implementar uma feature nova

1. Ler o componente Vue equivalente em `~/Repositories/passagem-express/src/components/steps/` para entender regra exata
2. Adicionar use case em `core/domain/usecase/` se faltar
3. Criar módulo `feature/<nome>/` com mesma estrutura: `Screen.kt + ViewModel.kt + UiState.kt + navigation/`
4. Adicionar em `settings.gradle.kts` + `app/build.gradle.kts`
5. Registrar a rota em `TotemNavHost.kt`

---

## 12. Memory persistente (`~/.claude/projects/.../memory/`)

Para sessões futuras com Claude Code, há memórias salvas que cobrem:
- `project_overview.md`, `project_architecture.md`, `project_mvp_scope.md`, `project_totem_target.md`, `project_totem_setup.md`
- `reference_backend_api.md`, `reference_web_project.md`, `reference_design_files.md`, `reference_api_token.md`
- `feedback_naming_totem.md` (Totem, não Kiosk)
- `feedback_agp9_builtin_kotlin.md` (workarounds AGP 9 + Hilt + KSP)
- `feedback_dont_invent_versions.md` (verificar Maven Central antes de pinar)
- `feedback_git_identity.md`
