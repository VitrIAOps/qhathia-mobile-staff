## Why

Since Fases 35–42 every backend error is an `application/problem+json` body with an `errorCode` and a Spanish `detail`, and several fixes from the QA campaign depend on telling two errors with the same HTTP status apart: `INVALID_PAIRING_TOKEN` vs `PAIRING_TOKEN_EXPIRED`, `CURRENT_PASSWORD_INCORRECT` vs `SESSION_REVOKED` (both 401), `PRODUCT_VERSION_CONFLICT` vs `PRODUCT_IN_PENDING_ORDERS` (both 409). Today `NetworkResult.HttpError` keeps only the status and `toNetworkResult()` drops the body, so only the order transitions (which parse their own `OrderProblemDto`) can see the `errorCode`. This is the shared piece "P-1" of `docs/handoff/android-regresion-2026-11-03.md` (§1.2, §3.1 and §3.3), and it has to land before F-58, F-60, F-62, F-48, F-93, F-83, F-85 and F-87.

## What Changes

- `NetworkResult.HttpError` gains an optional `problem: ProblemDto? = null`. Existing constructions (`HttpError(404, "Not Found")`) keep compiling and mean "no body read".
- New `ProblemDto` in `core:network` with the fields the Staff fixes read: `errorCode`, `detail`, `currentVersion` (`PRODUCT_VERSION_CONFLICT`), `pendingOrders[] {id, orderCode}` (`PRODUCT_IN_PENDING_ORDERS`), `productId`, `productName` and `reason` (`PRODUCT_NOT_SELLABLE`). All optional; a missing, empty or malformed body degrades to `problem = null`.
- New `suspend fun <T> Result<T>.toNetworkResult(json: Json)` overload that behaves like today's `toNetworkResult()` and, for a `ResponseException`, also reads the ProblemDetail body best-effort.
- `ProductApiService`, `DeviceApiService` and `AuthApiService` use the new overload on every call. `DeviceApiService` and `AuthApiService` receive the shared `Json` through `NetworkModule`, as `ProductApiService` and `OrderApiService` already do.
- No repository, domain or UI change: the error mapping in `core:data` keeps reading `code` only, so no message the user sees changes. The consuming changes (pairing, devices, auth, product edit/delete) switch their mapping to `problem?.errorCode` one by one.
- The Android standards doc (`NetworkResult` section) is updated to describe the new field and overload.

## Capabilities

### New Capabilities

- `network-error-details`: how the network layer exposes the backend's ProblemDetail (`errorCode`, `detail` and the per-error extra fields) on an HTTP error, and how it degrades when the body is missing or unreadable.

### Modified Capabilities

_None._ No user-facing requirement changes in this change.

## Impact

- **`core:network`** — `NetworkResult.kt` (new field + overload), new `dto/ProblemDto.kt`, `service/ProductApiService.kt`, `service/DeviceApiService.kt`, `service/AuthApiService.kt` (constructor gains `json: Json` for the last two), `di/NetworkModule.kt`. Tests: new `NetworkResultTest`, updated `DeviceApiServiceTest`, `AuthApiServiceTest`, `ProfileApiServicesTest` (constructor), plus ProblemDetail cases.
- **`docs/android/android-frontend-standards.md`** — `NetworkResult` section.
- **Not affected:** `OrderApiService` / `OrderMutationResult` / `OrderProblemDto` keep their own parsing (they already work); every other service keeps `toNetworkResult()` until a fix needs its `errorCode`; `core:data`, `core:domain`, features, the API.
