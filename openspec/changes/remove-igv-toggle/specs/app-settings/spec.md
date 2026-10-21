## MODIFIED Requirements

### Requirement: Configuración screen layout

The Configuración screen SHALL show a gradient top bar with back and the title "Configuración", and a card containing:
- **Idioma**: a two-option selector, "Español" and "English", with the selected option filled in the primary colour.

The screen SHALL NOT show any price or tax setting. The card SHALL NOT depend on the store being loaded: if reading the store fails, the screen SHALL look and behave exactly as when it succeeds, with no error row or retry.

#### Scenario: Screen shown
- **WHEN** the merchant opens Configuración
- **THEN** the Idioma selector is shown and no IGV toggle is shown

#### Scenario: Store fails to load
- **WHEN** `GET /api/v1/stores/{storeId}` fails while Configuración is open
- **THEN** no error or retry is shown and the language selector still works

## REMOVED Requirements

### Requirement: IGV display setting

**Reason**: The backend retired `pricesIncludeIgv` and `igvRate` from `GET/PUT /api/v1/stores/{storeId}` (api-contract §27, in production since 2026-10-18). The flag never recalculated a price — it only chose a line of PDF text — and in Peru consumer prices include IGV by law, so every store loaded them that way. The toggle had no observable effect and was reported as broken (BUG-012).

**Migration**: None for merchants: prices are always read as IGV-included. The app no longer sends or reads the field; an older build that still sends it receives `200` because the backend ignores unknown properties.
