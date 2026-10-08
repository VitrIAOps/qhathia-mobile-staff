package com.qhathia.mobile.staff.core.ui.component.productpreview

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The rules behind zooming and dragging a preview, which are arithmetic rather than rendering: how
 * a design is fitted, how far it may be zoomed, and that it can never be dragged out of view.
 */
class ProductPreviewZoomTest {

    /** A 1000×500 preview fitted into a 500×500 canvas: half size, so 500×250 on screen. */
    private fun PreviewTransform.step(zoomChange: Float = 1f, panX: Float = 0f, panY: Float = 0f) =
        transformedBy(
            zoomChange = zoomChange,
            panX = panX,
            panY = panY,
            fittedWidth = 500f,
            fittedHeight = 250f,
            containerWidth = 500f,
            containerHeight = 500f,
        )

    @Test
    fun `a wide design is fitted by its width`() {
        assertEquals(0.5f, fitScale(1000f, 500f, 500f, 500f))
    }

    @Test
    fun `a tall design is fitted by its height`() {
        assertEquals(0.25f, fitScale(500f, 2000f, 500f, 500f))
    }

    @Test
    fun `a small design is scaled up to fill the canvas`() {
        assertEquals(2f, fitScale(100f, 250f, 500f, 500f))
    }

    @Test
    fun `nothing is drawn before the canvas has a size`() {
        assertEquals(0f, fitScale(1000f, 500f, 0f, 500f))
    }

    @Test
    fun `pinching out zooms in`() {
        assertEquals(2f, PreviewTransform().step(zoomChange = 2f).zoom)
    }

    @Test
    fun `zoom stops at the maximum`() {
        assertEquals(MAX_PREVIEW_ZOOM, PreviewTransform(zoom = 3f).step(zoomChange = 5f).zoom)
    }

    @Test
    fun `a fitted preview cannot be pinched smaller`() {
        assertEquals(MIN_PREVIEW_ZOOM, PreviewTransform().step(zoomChange = 0.5f).zoom)
    }

    @Test
    fun `a fitted preview does not move when dragged`() {
        val dragged = PreviewTransform().step(panX = 120f, panY = -80f)

        assertEquals(0f, dragged.offsetX)
        assertEquals(0f, dragged.offsetY)
    }

    @Test
    fun `a zoomed preview follows the drag`() {
        // At 4x the preview is 2000x1000 in a 500x500 canvas: 750 and 250 of slack each way.
        val dragged = PreviewTransform(zoom = 4f).step(panX = 100f, panY = -50f)

        assertEquals(100f, dragged.offsetX)
        assertEquals(-50f, dragged.offsetY)
    }

    @Test
    fun `a drag stops at the preview's edges`() {
        val dragged = PreviewTransform(zoom = 4f).step(panX = 5000f, panY = -5000f)

        assertEquals(750f, dragged.offsetX)
        assertEquals(-250f, dragged.offsetY)
    }

    @Test
    fun `an axis that still fits the canvas stays centered while the other pans`() {
        // At 2x the preview is 1000x500: it overflows the width but exactly fills the height.
        val dragged = PreviewTransform(zoom = 2f).step(panX = 100f, panY = 100f)

        assertEquals(100f, dragged.offsetX)
        assertEquals(0f, dragged.offsetY)
    }

    @Test
    fun `zooming out pulls a dragged preview back into view`() {
        val zoomedOut = PreviewTransform(zoom = 4f, offsetX = 750f, offsetY = 250f).step(zoomChange = 0.25f)

        assertEquals(PreviewTransform(), zoomedOut)
    }

    @Test
    fun `double tap zooms a fitted preview in`() {
        val toggled = PreviewTransform().toggled()

        assertEquals(DOUBLE_TAP_PREVIEW_ZOOM, toggled.zoom)
        assertTrue(toggled.isZoomed)
    }

    @Test
    fun `double tap returns a zoomed preview to fitted and centered`() {
        val toggled = PreviewTransform(zoom = 3f, offsetX = 40f, offsetY = -10f).toggled()

        assertEquals(PreviewTransform(), toggled)
        assertFalse(toggled.isZoomed)
    }
}
