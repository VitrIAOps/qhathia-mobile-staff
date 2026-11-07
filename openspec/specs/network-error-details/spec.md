# network-error-details Specification

## Purpose
TBD - created by archiving change problem-detail-errors. Update Purpose after archive.
## Requirements
### Requirement: HTTP errors carry the backend's ProblemDetail

When a call made through the product, device or auth API services fails with a non-2xx response, the network layer SHALL return an HTTP error that holds the status code, the status description and, when the response body is a ProblemDetail, its `errorCode` and `detail`. The network layer SHALL also expose the extra fields the Staff app reacts to: `currentVersion` (`PRODUCT_VERSION_CONFLICT`), `pendingOrders` with each order's `id` and `orderCode` (`PRODUCT_IN_PENDING_ORDERS`), and `productId`, `productName` and `reason` (`PRODUCT_NOT_SELLABLE`). Fields the body does not carry SHALL be `null` (or an empty list for `pendingOrders`), and unknown fields SHALL be ignored.

#### Scenario: Error code read from a 401

- **WHEN** `POST /api/v1/auth/change-password` answers `401` with `{"errorCode":"CURRENT_PASSWORD_INCORRECT","detail":"La contraseña actual no es correcta"}`
- **THEN** the result is an HTTP error with code 401, `errorCode` `CURRENT_PASSWORD_INCORRECT` and that `detail`

#### Scenario: Version conflict on a product update

- **WHEN** `PUT /api/v1/products/{id}` answers `409` with `{"errorCode":"PRODUCT_VERSION_CONFLICT","detail":"Otra persona modificó este producto…","currentVersion":8}`
- **THEN** the result is an HTTP error with code 409, `errorCode` `PRODUCT_VERSION_CONFLICT` and `currentVersion` 8

#### Scenario: Pending orders on a product delete

- **WHEN** `DELETE /api/v1/products/{id}` answers `409` with `errorCode` `PRODUCT_IN_PENDING_ORDERS` and `pendingOrders: [{"id":"o1","orderCode":"400776"}]`
- **THEN** the HTTP error carries one pending order with id `o1` and order code `400776`

#### Scenario: Same status, different error codes

- **WHEN** one pairing attempt answers `400` with `errorCode` `INVALID_PAIRING_TOKEN` and another answers `400` with `errorCode` `PAIRING_TOKEN_EXPIRED`
- **THEN** the two HTTP errors have the same code and different `errorCode` values

### Requirement: Missing or unreadable error bodies degrade safely

Reading the ProblemDetail SHALL be best effort. When the error body is empty, is not JSON, or does not match the ProblemDetail shape, the network layer SHALL still return the HTTP error with its status code and description and with no ProblemDetail, and SHALL NOT turn it into a network error or throw. A failure with no HTTP response (timeout, no connection) SHALL remain a network error.

#### Scenario: Empty error body

- **WHEN** a call answers `500` with an empty body
- **THEN** the result is an HTTP error with code 500 and no ProblemDetail

#### Scenario: Non-JSON error body

- **WHEN** a call answers `502` with an HTML page from the proxy
- **THEN** the result is an HTTP error with code 502 and no ProblemDetail

#### Scenario: No connection

- **WHEN** a call fails with an `IOException` before any response
- **THEN** the result is a network error, as before

### Requirement: Existing error handling is unchanged

Adding the ProblemDetail SHALL NOT change how the data layer classifies errors in this change: every repository SHALL keep mapping an HTTP error by its status code exactly as before, so no message, retry or navigation the merchant sees changes until a later change reads the `errorCode`.

#### Scenario: Repository mapping keeps using the status

- **WHEN** `GET /api/v1/devices` answers `401` with a ProblemDetail body
- **THEN** the device repository returns the same domain error it returned before this change

