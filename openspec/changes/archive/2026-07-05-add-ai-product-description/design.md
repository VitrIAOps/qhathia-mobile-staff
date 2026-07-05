## Context

Product registration is already implemented as a vertical slice across `features:product-register`, `core:domain`, `core:data` and `core:network`. The registration screen already has a "Generate with AI" button and a `GenerateDescriptionWithAi` event, but the ViewModel handles it as a no-op (`ProductRegisterViewModel.kt:64-65`). Images are already picked as `SelectedImage(uri, mimeType)` and compressed off-thread by `core:data`'s `ImageCompressor` (JPEG, ≤500 KB) before the registration multipart call.

The backend exposes two fail-open LLM endpoints (api-contract §8.7, §8.8), both on the authenticated client and requiring an active store:

- `POST /api/v1/products/draft-from-image` — multipart, single `image` part (≤5 MB), returns `{ description, categoryId, tags, source }`. `categoryId` may be `null` or a store category UUID; `tags` are a closed pre-validated subset. On LLM failure it still returns 200 with empty/`stub` payload.
- `POST /api/v1/products/regenerate-description` — JSON `{ currentDescription }` (≤2000 chars), returns `{ description }`. On LLM failure it echoes the original. `422` if `currentDescription` is blank/too long.

This change makes the AI button functional and wires both endpoints through the existing Clean Architecture layers. No new Gradle modules, permissions, or manifest changes are required.

## Goals / Non-Goals

**Goals:**
- Draft description + category + tags from the first loaded image (`draft-from-image`).
- Optimize an already-written description (`regenerate-description`) when the form's AI fields are all filled.
- Warn before overwriting existing description/tags/category during drafting.
- Keep the two endpoints' fail-open semantics visible to the merchant as "nothing changed", never as an error; only a genuine transport failure surfaces a transient error.
- Reuse existing patterns: `NetworkResult` → `Result` mapping at the data boundary, MVI on the screen, `ImageCompressor` for the drafting image.

**Non-Goals:**
- Triggering AI automatically on image load (it stays an explicit merchant action, matching the contract's "front presses Generate" note).
- Running drafting on images beyond the first, or a multi-image "pick which photo" UX.
- Persisting drafts server-side (both endpoints are stateless; persistence still happens only at `with-images` submit).
- Changing the existing registration/submit flow or its spec.

## Decisions

**Module boundaries (no new modules).** The work extends the four existing modules along the established dependency graph `features → domain ← data → network`:

- `core:network` — add `DraftFromImageResponseDto`, `RegenerateDescriptionRequestDto`, `RegenerateDescriptionResponseDto` (all `@Serializable`) to `ProductDtos.kt`, and two methods on the existing `ProductApiService`: `draftFromImage(image: MultipartImage): NetworkResult<DraftFromImageResponseDto>` (reusing the `MultipartImage`/`formData` pattern already in `createProductWithImages`) and `regenerateDescription(body): NetworkResult<RegenerateDescriptionResponseDto>`. Both run on the injected authenticated `HttpClient`.
- `core:domain` — add domain models `ProductDraft(description, categoryId, tags)` and a plain `String` result for optimization; add a repository port and two use cases `DraftProductFromImageUseCase` and `OptimizeDescriptionUseCase`.
- `core:data` — implement the port, mapping `NetworkResult → Result`. Drafting compresses the chosen `SelectedImage` with the existing `ImageCompressor` (its ≤500 KB output is well within the endpoint's 5 MB cap) and wraps it as `MultipartImage`.
- `features:product-register` — extend `ProductRegisterUiState`/`UiEvent`, implement the event in the ViewModel, and render the dialog + dynamic button in `ProductRegisterContent`.

*Alternative considered:* adding the two methods to the existing `ProductRepository` port instead of a new `ProductAiRepository`. Chosen: a **separate `ProductAiRepository` port** so the AI concern stays cohesive and independently testable/fakeable, matching the one-repository-per-concern grain already used (`AuthRepository`, `ProductRepository`). The `RepositoryImpl` can still live in `core:data` beside `ProductRepositoryImpl`.

**Generate vs. optimize is derived state, not a mode toggle.** The button label and behavior derive from the form: if `description`, `selectedCategoryId` and parsed tags are all non-empty → "Optimize with AI" → `regenerate-description`; otherwise → "Generate with AI" → `draft-from-image`. This mirrors how `isSubmitEnabled` is already a derived getter on `ProductRegisterUiState`, so no extra mode field is stored.

**Warning dialog is drafting-only and state-driven.** A new `showAiReplaceDialog` flag on the UiState gates the drafting overwrite. The `GenerateDescriptionWithAi` event: if optimize state → run optimize directly; else if any of description/tags/category is non-empty → set `showAiReplaceDialog = true`; else run drafting immediately. Confirm/dismiss events (`ConfirmAiReplace`/`DismissAiReplace`) mirror the existing discard-dialog pattern.

**Fail-open handling lives in the ViewModel mapping.** The repository returns the raw domain result; the ViewModel treats an empty draft (blank description, null category, empty tags) and an echoed optimization as "apply what came back" — which is a no-op — and never emits an error for them. Category is applied only when `categoryId` is in the currently loaded `categories`; tags are joined back into the comma-separated `tags` string the form already uses (`parseTags` is the inverse). Only a `NetworkResult.NetworkError`/transport failure maps to a transient `ShowError` effect.

**AI in-flight state is separate from submit.** Add `isAiLoading: Boolean` to the UiState (distinct from `isSubmitting`) so the AI action shows its own spinner and disables while a request runs, without blocking or being blocked by the submit path.

## Risks / Trade-offs

- **[Category from AI never matches the store's categories]** → applied defensively: unmatched `categoryId` simply leaves the selection unchanged (per spec + contract's defense-in-depth), so no invalid category id can reach submit.
- **[Merchant loses hand-typed description/tags to a draft]** → mitigated by the replace-confirmation dialog before any overwrite; optimization only ever touches the description, which is the field the merchant explicitly asked to rewrite.
- **[Fail-open looks like a broken button ("I pressed it and nothing happened")]** → acceptable and matches the contract's intent; the loading indicator gives feedback that work happened. A future enhancement could show a subtle "couldn't improve" hint, tracked as an open question rather than scoped here.
- **[Drafting uses only the first image]** → matches the API contract note; deliberate to keep the interaction one-tap. Re-running on a different photo is out of scope.
- **[`regenerate-description` 422 on blank input]** → cannot occur because optimize state requires a non-empty description; drafting (not optimize) is used when description is empty.

## Open Questions

- Should a fail-open (empty/stub) draft show a soft, non-error hint ("AI couldn't read this photo, fill it in manually") instead of silently changing nothing? Deferred; current spec keeps it silent per the contract.
