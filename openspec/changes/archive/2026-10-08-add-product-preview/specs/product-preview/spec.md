## ADDED Requirements

### Requirement: Minimum data for a preview

A product preview SHALL only be available when the form it is opened from holds a valid product name, a valid price greater than zero and at least one photo. No other field SHALL be required. The preview SHALL NOT be reachable while that minimum is not met.

#### Scenario: Minimum data present

- **WHEN** the form holds a valid name, a valid price and at least one photo, and every other field is empty
- **THEN** the preview can be opened

#### Scenario: Name missing

- **WHEN** the form holds a valid price and a photo but the name is blank
- **THEN** the preview cannot be opened

#### Scenario: Price missing or invalid

- **WHEN** the form holds a name and a photo but the price is empty, zero or not a valid amount
- **THEN** the preview cannot be opened

#### Scenario: No photo

- **WHEN** the form holds a valid name and price but no photo
- **THEN** the preview cannot be opened

### Requirement: Full-screen preview dialog

The preview SHALL open as a full-screen dialog over the form, titled `Vista previa`, with a close action. Closing it, by the close action or system back, SHALL return to the form with every entered value intact. Opening or closing the preview SHALL NOT save the product, issue any request, or mark the form as having unsaved changes.

#### Scenario: Opening the preview

- **WHEN** the merchant opens the preview
- **THEN** a full-screen dialog titled `Vista previa` covers the form

#### Scenario: Closing with the close action

- **WHEN** the merchant taps the close action
- **THEN** the dialog closes and the form shows the same values as before

#### Scenario: Closing with system back

- **WHEN** the merchant presses system back while the preview is open
- **THEN** only the preview closes; the form stays open and no discard dialog is shown

#### Scenario: Nothing is persisted

- **WHEN** the merchant opens and closes the preview
- **THEN** no network request has been issued and the form's unsaved-changes state is unchanged

### Requirement: Preview reflects the current form values

The preview SHALL be built from the values on the form at the moment it is opened, including values that have not been saved: the name, the price, the description, the selected category's name, the stock and the photos, with the first photo as the primary one. When a discount is applied on the form, the price shown SHALL be the price with the discount applied.

#### Scenario: Unsaved values are shown

- **WHEN** the merchant types a new name and opens the preview without saving
- **THEN** the preview shows the new name

#### Scenario: Primary photo

- **WHEN** the form holds several photos
- **THEN** the item previews show the first photo and the detail preview shows the first photo as the main image with the photos listed as thumbnails

#### Scenario: Price in soles

- **WHEN** the form's price is 380
- **THEN** the preview shows `S/ 380.00`

#### Scenario: Discounted price

- **WHEN** the form's price is 189.00 with a 20 % discount applied
- **THEN** the preview shows `S/ 151.20`

### Requirement: Item and detail tabs

The dialog SHALL offer two tabs, `Item` and `Detalle`, and SHALL open on `Item`. The `Item` tab SHALL show the product as a Qhathia Discovery catalog list item; the `Detalle` tab SHALL show the Qhathia Discovery product detail screen.

#### Scenario: Default tab

- **WHEN** the preview opens
- **THEN** the `Item` tab is selected

#### Scenario: Switching to the detail

- **WHEN** the merchant selects the `Detalle` tab
- **THEN** the product detail preview replaces the item preview

### Requirement: Item orientation

On the `Item` tab the dialog SHALL offer a `Vertical` / `Horizontal` orientation switch, `Vertical` by default. `Vertical` SHALL show the vertical list item — photo on top, then the name, then the price with the add-to-cart control. `Horizontal` SHALL show the horizontal list item — photo at the start, with the category name, the product name and the price beside it.

#### Scenario: Vertical item

- **WHEN** the `Item` tab is shown with `Vertical` selected
- **THEN** the vertical list item is shown with the primary photo, the name and the price

#### Scenario: Horizontal item

- **WHEN** the merchant selects `Horizontal` on the `Item` tab
- **THEN** the horizontal list item is shown with the primary photo, the category name, the name and the price

### Requirement: Detail preview is always landscape

The `Detalle` tab SHALL always render the product detail in its landscape layout, whatever the device orientation, showing the main photo, the photo thumbnails, the name, the available stock, the price and the description. The orientation switch SHALL NOT allow a vertical detail. While the device is held in portrait, the tab SHALL show a hint inviting the merchant to rotate the phone.

#### Scenario: Detail in portrait

- **WHEN** the `Detalle` tab is shown with the device in portrait
- **THEN** the landscape detail layout is shown scaled to fit, together with a hint to rotate the phone

#### Scenario: Detail in landscape

- **WHEN** the `Detalle` tab is shown with the device in landscape
- **THEN** the landscape detail layout fills the available area and the rotate hint is not shown

#### Scenario: Orientation cannot be changed

- **WHEN** the `Detalle` tab is selected
- **THEN** the orientation switch is locked on `Horizontal`

### Requirement: Previews are non-interactive

Every preview SHALL behave as a rendered image: none of the Discovery controls drawn inside it — the add-to-cart, share and quantity controls, the photo thumbnails, or the navigation actions — SHALL react to taps, and the preview SHALL be exposed to accessibility services as a single described image rather than as individual controls.

#### Scenario: Tapping a control inside the preview

- **WHEN** the merchant taps the add-to-cart control drawn in a preview
- **THEN** nothing happens

#### Scenario: Screen reader

- **WHEN** a screen reader focuses the preview
- **THEN** it announces one description of the previewed product instead of the controls drawn inside it

### Requirement: Zoom and pan

The merchant SHALL be able to zoom every preview in with a pinch gesture, up to a maximum zoom, and pan it while zoomed, without the content being dragged out of view. A double tap SHALL toggle between the fitted size and a zoomed size. Changing the tab or the orientation SHALL reset the preview to the fitted size.

#### Scenario: Pinch to zoom

- **WHEN** the merchant pinches out on a preview
- **THEN** the preview is enlarged, up to the maximum zoom

#### Scenario: Pan while zoomed

- **WHEN** the preview is zoomed in and the merchant drags it
- **THEN** the preview moves with the drag and stops at its edges

#### Scenario: Never smaller than fitted

- **WHEN** the merchant pinches in on a fitted preview
- **THEN** the preview stays at the fitted size

#### Scenario: Double tap

- **WHEN** the merchant double-taps a fitted preview
- **THEN** the preview zooms in, and a second double tap returns it to the fitted size

#### Scenario: Zoom resets on view change

- **WHEN** the merchant zooms in and then changes the tab or the orientation
- **THEN** the newly shown preview is at the fitted size

### Requirement: Device rotation

The preview dialog SHALL work in both portrait and landscape device orientations, and rotating the device SHALL keep the dialog open on the same tab and item orientation.

#### Scenario: Rotating with the preview open

- **WHEN** the merchant rotates the device while viewing the `Detalle` tab
- **THEN** the dialog stays open on the `Detalle` tab

### Requirement: Missing data placeholder

Any preview element whose form value has not been entered — the description, the category name, or the stock — SHALL show the text `DATO NO INGRESADO` in its place.

#### Scenario: Description not entered

- **WHEN** the description is blank and the detail preview is shown
- **THEN** the description area shows `DATO NO INGRESADO`

#### Scenario: Category not selected

- **WHEN** no category is selected and the horizontal item preview is shown
- **THEN** the category label shows `DATO NO INGRESADO`

#### Scenario: Stock not entered

- **WHEN** the stock field is empty or invalid and the detail preview is shown
- **THEN** the availability line shows `DATO NO INGRESADO`

#### Scenario: Stock entered

- **WHEN** the stock is 22 and the detail preview is shown
- **THEN** the availability line shows `22 disponibles`

### Requirement: Related products are mocked and blurred

The detail preview SHALL show the "Podrías acompañar este producto con…" section filled with mocked placeholder items that are blurred or otherwise obscured so they cannot be read as real products. The app SHALL NOT request related products.

#### Scenario: Related section in the detail preview

- **WHEN** the detail preview is shown
- **THEN** the related-products section shows obscured placeholder items and no request for related products is issued

#### Scenario: Device without blur support

- **WHEN** the detail preview is shown on a device that cannot render blur
- **THEN** the related-products items are still obscured, with no legible mock text
