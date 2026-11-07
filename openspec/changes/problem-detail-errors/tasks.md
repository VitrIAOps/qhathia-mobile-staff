## 1. ProblemDetail DTO (`core:network`)

- [x] 1.1 Create `dto/ProblemDto.kt` with `ProblemDto(errorCode, detail, currentVersion, pendingOrders, productId, productName, reason)` and `PendingOrderRefDto(id, orderCode)`, every field optional (`pendingOrders` defaults to `emptyList()`), per design decision 2. KDoc: the RFC 7807 envelope shared by auth, biz and vo; which `errorCode` carries each extra field (API_REFERENCE §39, §40); unknown keys are ignored

## 2. NetworkResult (`core:network`)

- [x] 2.1 In `NetworkResult.kt`, add `val problem: ProblemDto? = null` to `HttpError`; KDoc: `null` means the body was not read, was empty or was not a ProblemDetail
- [x] 2.2 Add `suspend fun <T> Result<T>.toNetworkResult(json: Json): NetworkResult<T>` next to the existing one: same mapping, plus `problem = response.readProblem(json)` for a `ResponseException`
- [x] 2.3 Add the private `HttpResponse.readProblem(json)`: decode `bodyAsText()` best effort, return `null` on any failure, and rethrow `CancellationException`
- [x] 2.4 Leave the non-suspend `toNetworkResult()` unchanged and point its KDoc to the new overload

## 3. Services (`core:network`)

- [x] 3.1 `ProductApiService`: replace every `.toNetworkResult()` with `.toNetworkResult(json)` (it already receives `json`)
- [x] 3.2 `DeviceApiService`: add `private val json: Json` to the constructor and use `.toNetworkResult(json)` on every call
- [x] 3.3 `AuthApiService`: add `private val json: Json` to the constructor and use `.toNetworkResult(json)` on every call (public and authenticated)
- [x] 3.4 `di/NetworkModule.kt`: pass the shared `Json` to `provideDeviceApiService` and `provideAuthApiService`
- [x] 3.5 Confirm no other caller constructs `DeviceApiService` or `AuthApiService` outside `NetworkModule` and tests (grep), and that `core:data` repositories compile unchanged

## 4. Tests (`core:network`)

- [x] 4.1 New `NetworkResultTest` (MockEngine client with `expectSuccess = true`): 409 `PRODUCT_VERSION_CONFLICT` with `currentVersion` → fields read; 409 `PRODUCT_IN_PENDING_ORDERS` → `pendingOrders` read; extra envelope keys (`type`, `title`, `status`, `instance`, `timestamp`) ignored; empty body → `problem == null` with the status kept; HTML body → `problem == null`; `IOException` → `NetworkError`; a success → `Success`
- [x] 4.2 ~~`NetworkResultTest`: cancellation while reading the body~~ — not unit-testable with `MockEngine` (the body is already buffered, so `bodyAsText()` never suspends). The rethrow of `CancellationException` in `readProblem` is checked in code review instead
- [x] 4.3 `DeviceApiServiceTest`: update `serviceReturning` to pass `Json`; add a `pairDevice` 400 with `errorCode` `PAIRING_TOKEN_EXPIRED` → `HttpError.problem.errorCode` read; keep the existing cases passing
- [x] 4.4 `AuthApiServiceTest` and `ProfileApiServicesTest`: pass `Json` to `AuthApiService`; add a `changePassword` 401 `CURRENT_PASSWORD_INCORRECT` case → `errorCode` and `detail` read
- [x] 4.5 New `ProductApiServiceTest` with one `updateProduct` 409 `PRODUCT_VERSION_CONFLICT` case and one `deleteProduct` 409 `PRODUCT_IN_PENDING_ORDERS` case

## 5. Docs

- [x] 5.1 `docs/android/android-frontend-standards.md`, `NetworkResult` section: show the `problem` field and the `toNetworkResult(json)` overload, and state the rule — read `problem?.errorCode` before the status code when two errors share a status

## 6. Verification

- [x] 6.1 Run `./gradlew :core:network:testDebugUnitTest`, then `./gradlew :core:data:testDebugUnitTest` (one build at a time); all pass with no change to `core:data` tests
- [x] 6.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 7. Manual — QA

- [ ] 7.1 Manual — Smoke test against the QA store: login, Inventario, editing a product, Dispositivos and Cambiar contraseña behave and show the same messages as before (no visible change expected)
