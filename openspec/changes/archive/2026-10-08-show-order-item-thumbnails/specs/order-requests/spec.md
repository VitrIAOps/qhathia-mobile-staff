## ADDED Requirements

### Requirement: Show each item's product photo in the order detail

Each item row of the order detail dialog SHALL display the product photo given by `items[].productImageUrl` of `GET /api/v1/orders/{id}` in its thumbnail slot. When `productImageUrl` is `null` or blank, or when the image cannot be loaded, the row MUST display the neutral placeholder instead of an empty slot. The photo MUST come from the detail response alone — the app SHALL NOT issue any per-item product request to obtain it. The photo is the product's current primary image, not a snapshot taken when the order was created, so the app SHALL NOT cache or persist the URL beyond the detail it arrived with.

#### Scenario: Item with a product photo
- **WHEN** the detail response carries an item whose `productImageUrl` is a valid image URL
- **THEN** that item's thumbnail shows the product photo, cropped to fill the slot

#### Scenario: Item whose product has no images
- **WHEN** the detail response carries an item whose `productImageUrl` is `null`
- **THEN** that item's thumbnail shows the neutral placeholder

#### Scenario: Blank image URL
- **WHEN** the detail response carries an item whose `productImageUrl` is an empty or blank string
- **THEN** that item's thumbnail shows the neutral placeholder and no image request is attempted

#### Scenario: Image fails to load
- **WHEN** an item has a `productImageUrl` but the image cannot be downloaded or decoded
- **THEN** that item's thumbnail shows the neutral placeholder, and the rest of the dialog — items, total and actions — remains usable

#### Scenario: Photo is not required to act on the order
- **WHEN** an item's photo is still loading
- **THEN** the item's name, price, quantity and line total are already shown and the confirm and reject actions are available

#### Scenario: No per-item request for the photo
- **WHEN** the merchant opens the detail of an order with several items
- **THEN** only `GET /api/v1/orders/{id}` is issued against the backend API, and no product endpoint is called per item

#### Scenario: Response without the field
- **WHEN** the detail response omits `productImageUrl` for its items
- **THEN** the dialog renders normally with the neutral placeholder on every item

#### Scenario: Reopening shows the current photo
- **WHEN** the merchant changes a product's primary photo and then reopens the detail of an order containing that product
- **THEN** the item shows the photo returned by the new detail response
