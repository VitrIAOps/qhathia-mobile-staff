## Context

`core:network` maps every Ktor call to `NetworkResult` with `Result<T>.toNetworkResult()`. The client is built with `expectSuccess = true` (`KtorClientFactory.kt`), so a non-2xx response throws a `ResponseException`, and `toNetworkResult()` turns it into `HttpError(code, message)` — the body is never read.

The backend answers every error as `application/problem+json` with `errorCode` and a Spanish `detail`, including the 401/403 raised by the security filter (API_REFERENCE §42). Some errors carry extra fields: `currentVersion` (§40, `PRODUCT_VERSION_CONFLICT`), `pendingOrders[] {id, orderCode}` (§39, `PRODUCT_IN_PENDING_ORDERS`), and `productId`/`productName`/`reason` (§39, `PRODUCT_NOT_SELLABLE`).

There is already one working precedent: `OrderApiService` catches the `ResponseException` itself and decodes `OrderProblemDto` from `response.bodyAsText()` (`OrderApiService.kt:85-94`), returning its own `OrderMutationResult`. That proves the body is still readable after Ktor's default validation on this client.

The regression handoff describes this piece twice with two shapes: §3.1 adds `errorCode`/`detail` as flat fields of `HttpError` and a `toNetworkResultWithProblem(json)`; §3.3 adds `problem: ProblemDto?` and a `toNetworkResult(json)` overload. Both sections say it must be built once.

Modules involved — no new module, dependency graph unchanged:

```
core:data ──► core:network   (repositories keep reading HttpError.code in this change)
                 ├─ NetworkResult.kt      + problem field, + toNetworkResult(json)
                 ├─ dto/ProblemDto.kt     new
                 ├─ service/{Product,Device,Auth}ApiService.kt
                 └─ di/NetworkModule.kt   inject Json into Device/Auth services
```

## Goals / Non-Goals

**Goals:**
- One shared way, in `core:network`, to get a ProblemDetail's `errorCode`, `detail` and extra fields from any failed call.
- Product, device and auth calls expose it, because the next changes need it there (F-58, F-60, F-62, F-48, F-93, F-83, F-85, F-87).
- Zero behavior change for the merchant: same errors, same messages.

**Non-Goals:**
- Changing any repository mapping, domain error type or UI message. Each consuming change does that for its own flow.
- Migrating `OrderApiService` / `OrderMutationResult` / `OrderProblemDto` to the shared DTO. They work; folding them in can happen when an order fix touches that code.
- Switching the other services (catalog, dashboard, insights, user, store, subscription, notification, category). They move when a fix needs their `errorCode` — a one-line change each.
- Handling `SESSION_REVOKED` / `CURRENT_PASSWORD_INCORRECT` in the bearer refresh (that is F-48).

## Decisions

### 1. `HttpError.problem: ProblemDto? = null` (the §3.3 shape), not flat `errorCode`/`detail`

`ProblemDto` holds `errorCode` and `detail` plus the per-error extras. Callers read `problem?.errorCode`.

- *Why:* the product fixes need more than two fields (`currentVersion`, `pendingOrders`). Flat fields on `HttpError` would grow with every new error shape; a nested DTO keeps `NetworkResult` stable and lets the DTO grow alone. It also covers everything §3.1 needs.
- *Alternative:* §3.1's flat `errorCode`/`detail`. Rejected: forces a second carrier for the extras, which is the duplication the handoff warns against.
- The default `null` keeps every existing `HttpError(code, message)` — in production code, fakes and ~30 tests — compiling with the same meaning ("body not read").

### 2. One `ProblemDto` for all services, every field optional

```kotlin
@Serializable
data class ProblemDto(
    val errorCode: String? = null,
    val detail: String? = null,
    val currentVersion: Long? = null,
    val pendingOrders: List<PendingOrderRefDto> = emptyList(),
    val productId: String? = null,
    val productName: String? = null,
    val reason: String? = null,
)

@Serializable
data class PendingOrderRefDto(val id: String? = null, val orderCode: String? = null)
```

- *Why one DTO:* the envelope is the same in auth, biz and vo; only some errors add fields. The shared `Json` has `ignoreUnknownKeys = true` and `coerceInputValues = true`, so `type`, `title`, `status`, `instance`, `timestamp` and any future field are ignored, and a wrong-typed field falls back to its default instead of failing the whole parse.
- `PendingOrderRefDto.id` is nullable too (unlike the handoff sketch) so one malformed entry cannot drop the whole ProblemDetail, and with it the `errorCode`.
- *Alternative:* one DTO per error family. Rejected: the caller would need to know which DTO to decode before knowing the `errorCode`.

### 3. A `suspend` overload `Result<T>.toNetworkResult(json: Json)` next to the existing one

```kotlin
suspend fun <T> Result<T>.toNetworkResult(json: Json): NetworkResult<T> = fold(
    onSuccess = { NetworkResult.Success(it) },
    onFailure = { t ->
        if (t is ResponseException) NetworkResult.HttpError(
            code = t.response.status.value,
            message = t.response.status.description,
            problem = t.response.readProblem(json),
        ) else NetworkResult.NetworkError(t)
    },
)

private suspend fun HttpResponse.readProblem(json: Json): ProblemDto? =
    runCatching { json.decodeFromString<ProblemDto>(bodyAsText()) }.getOrNull()
```

- `Result.fold` is `inline`, so calling the suspend `bodyAsText()` inside the lambda is allowed. `runCatching` makes every read/parse failure degrade to `problem = null`.
- `CancellationException` must not be swallowed: `runCatching` around `bodyAsText()` would catch it. `readProblem` rethrows `CancellationException` explicitly so a cancelled screen still cancels.
- *Why an overload and not `toNetworkResultWithProblem`:* migrating a call is just adding `(json)`; the name says the same thing in both forms. The non-suspend `toNetworkResult()` stays for services that do not inject `Json`.
- *Alternative:* a Ktor `HttpResponseValidator` that parses the body into a custom exception. Rejected: it changes the client for every call (orders included), and `NetworkResult` would still need the field.

### 4. Migrate every call of the three services, not only the ones the next fixes list

`ProductApiService`, `DeviceApiService` and `AuthApiService` use `toNetworkResult(json)` on every method.

- *Why:* reading the body costs nothing on success (it only happens on error), and it avoids each later change having to remember to migrate "its" call. A consistent rule per service is easier to review than a per-method list.
- `DeviceApiService(client, json)` and `AuthApiService(publicClient, authenticatedClient, json)` get `Json` through `NetworkModule`, matching `ProductApiService` and `OrderApiService`.

### 5. Repositories untouched

`core:data` keeps `when (result.code)` everywhere. This change is a pure enabler, so it can merge alone, and each consuming change gets a focused diff on its own mapping and its own tests.

## Risks / Trade-offs

- [Body already consumed when the exception is built] → The order flow reads it today on the same client; a `MockEngine` test with `expectSuccess = true` per service guards it.
- [Swallowing cancellation inside `runCatching`] → `readProblem` rethrows `CancellationException`; covered by a unit test.
- [A 401 passes through the bearer `Auth` plugin first] → Unchanged: the plugin still refreshes and retries before the error reaches `toNetworkResult(json)`. Only the final response's body is read. F-48 handles which 401s must not refresh.
- [Logging] → `ProblemDto` is never logged by this change; `detail` is server text with no secrets, but nothing new is printed.
- [Two ProblemDetail DTOs coexist (`OrderProblemDto` and `ProblemDto`)] → Accepted for now; noted as a follow-up when an order fix touches `OrderApiService`.

## Migration Plan

Single merge, no flag. Rollback is reverting the commit: no persisted data, no API change, no visible behavior change.

## Open Questions

_None._ The P-1 shape mismatch between §3.1 and §3.3 is resolved by decision 1.
