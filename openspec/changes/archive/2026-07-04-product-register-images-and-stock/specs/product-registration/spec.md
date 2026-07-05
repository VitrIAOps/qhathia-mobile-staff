## MODIFIED Requirements

### Requirement: Product quantity

The form SHALL provide a product quantity (stock) field. The quantity SHALL NOT be negative; a negative quantity SHALL block registration. The validated quantity SHALL be sent to the backend as the product's `stock` when the product is registered. The registration request SHALL NOT set `availability` explicitly, so that the backend derives it from the submitted stock per the API's `stock ↔ availability` sync rule.

#### Scenario: Negative quantity blocks registration

- **WHEN** the merchant enters a quantity less than zero
- **THEN** the quantity field shows a validation error and the form cannot be submitted

#### Scenario: Non-negative quantity is accepted

- **WHEN** the merchant enters a quantity of zero or greater
- **THEN** the quantity field is accepted and contributes to enabling submission

#### Scenario: Quantity is transmitted as stock

- **WHEN** the product is registered
- **THEN** the registration request includes the entered quantity as the product's `stock`, and does not send an explicit `availability` value

### Requirement: Image compression before upload

Each selected image SHALL be compressed on-device to at most 500 KB while preserving good quality before it is submitted to the create-product endpoint, keeping every image and the overall request within the endpoint's per-image and total size limits.

#### Scenario: Large image is compressed

- **WHEN** the merchant selects an image larger than 500 KB
- **THEN** the app re-encodes it to 500 KB or less before submitting it, keeping acceptable visual quality

#### Scenario: Image that cannot be compressed

- **WHEN** a selected image cannot be reduced to 500 KB or less
- **THEN** registration fails with an explanatory message and the form remains available

### Requirement: Product registration orchestration

On submit, the app SHALL register the product in a single atomic request to `POST /api/v1/products/with-images`, sending the product data (including `stock`, category, tags and price) as the `product` part and each compressed image as an `images` part, with the first image treated as primary. The app SHALL NOT create the product and attach media in separate calls, and SHALL NOT perform a client-side rollback, because the backend creates the product and its media atomically and rolls back entirely if any image upload fails.

#### Scenario: Successful registration

- **WHEN** the merchant submits a valid form
- **THEN** the app sends one multipart request with the product data and all images, and the registration is reported as successful when the backend returns the created product

#### Scenario: Backend rejects the registration atomically

- **WHEN** the backend fails to create the product or upload any image
- **THEN** no partial product is created, the app reports the registration as failed, and no client-side rollback call is made

### Requirement: Plan-limit handling

When product registration is rejected because the store's plan product limit has been reached (HTTP 402 from the create-with-images endpoint), the app SHALL NOT keep the merchant on the registration form; instead it SHALL return to the inventory list and surface a distinct plan-exceeded message.

#### Scenario: Plan limit reached

- **WHEN** registration is rejected because the plan's product limit is exceeded
- **THEN** the app returns to the inventory list and shows a message indicating the plan limit was exceeded

## REMOVED Requirements

### Requirement: Image upload to Cloudflare R2

**Reason**: The backend now owns image storage. The atomic `POST /api/v1/products/with-images` endpoint receives the photos in the multipart request and uploads them to Cloudflare R2 server-side, so the app no longer uploads to the bucket or handles R2 credentials. This removes the app-side R2 credentials that previously shipped inside the APK.

**Migration**: Selected images are submitted as `images` parts of the `POST /api/v1/products/with-images` request (see the modified "Product registration orchestration" requirement) instead of being PUT to R2. All R2 configuration (`R2_*` build config, the R2 Ktor client, the SigV4 signer, and the media-storage repository) is removed.
