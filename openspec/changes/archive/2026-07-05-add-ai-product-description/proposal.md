## Why

During product registration, filling in a good description, choosing a category and typing tags is the slowest, most friction-heavy part of the form for a merchant. The backend already exposes two LLM endpoints (`draft-from-image` and `regenerate-description`) that can do this work from the product photos the merchant already uploaded. Wiring them into the registration screen turns a manual chore into a one-tap action, directly improving the "100% mobile-operated, minimal learning curve" product goal.

## What Changes

- Make the existing (currently inert) "Generate with AI" action on the Product Registration screen functional: when at least one image is loaded, it calls `POST /api/v1/products/draft-from-image` with the first image and fills description, category and tags from the response.
- Category from the AI response is only applied when its `categoryId` matches one of the store's loaded categories; otherwise category is left unselected. Tags are applied as-is (backend returns a closed, pre-validated subset). Empty/`stub` responses (fail-open) leave fields untouched.
- Add a **warning dialog**: when description, tags or category already hold data and the merchant triggers AI generation, confirm that those fields will be replaced before overwriting them.
- **Optimize flow**: when description, category and tags are all already filled, the button relabels to "Optimize with AI" and calls `POST /api/v1/products/regenerate-description` with the current description, replacing only the description with the paraphrased result.
- Show a loading state on the AI action while a request is in flight and surface a transient error if the network call itself fails (endpoints are fail-open at the app layer otherwise).

## Capabilities

### New Capabilities
<!-- None — this extends the existing product-registration capability. -->

### Modified Capabilities
- `product-registration`: adds AI-assisted drafting of description/category/tags from a product image, the replace-confirmation warning, and the AI description optimization flow, plus the generate-vs-optimize button behavior and its loading/error handling.

## Impact

- **API contract**: consumes `POST /api/v1/products/draft-from-image` (multipart, one image, ≤5 MB) and `POST /api/v1/products/regenerate-description` (JSON `currentDescription`). Both require the merchant's Bearer token and an active store; both are fail-open (always 200 on LLM failure).
- **`core:network`**: new request/response DTOs and two `ProductApiService` methods on the authenticated client.
- **`core:domain`**: new `ProductAiRepository` port (or extension of `ProductRepository`), domain models for the draft/optimize results, and two use cases.
- **`core:data`**: repository implementation mapping the endpoints, reusing `ImageCompressor` to keep the drafting image within the 5 MB limit.
- **`features:product-register`**: `ProductRegisterUiState`/`UiEvent`/`ViewModel` gain AI generation state, the replace-warning dialog, and generate/optimize branching; `ProductRegisterContent` renders the dialog and the dynamic button label/loading.
- No new Gradle modules, permissions, or manifest changes (image picking/network already wired for registration).
