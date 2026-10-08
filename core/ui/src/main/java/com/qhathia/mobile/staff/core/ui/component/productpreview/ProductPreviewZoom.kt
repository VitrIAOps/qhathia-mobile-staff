package com.qhathia.mobile.staff.core.ui.component.productpreview

import kotlin.math.max
import kotlin.math.min

internal const val MIN_PREVIEW_ZOOM = 1f
internal const val MAX_PREVIEW_ZOOM = 4f
internal const val DOUBLE_TAP_PREVIEW_ZOOM = 2f

/**
 * How far the merchant has zoomed and dragged a preview. [zoom] multiplies the fitted size (1 = the
 * whole preview fits the canvas); the offsets are in pixels from the centered position.
 */
internal data class PreviewTransform(
    val zoom: Float = MIN_PREVIEW_ZOOM,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    val isZoomed: Boolean get() = zoom > MIN_PREVIEW_ZOOM
}

/** The uniform scale that makes a [contentWidth] × [contentHeight] design fit the container. */
internal fun fitScale(
    contentWidth: Float,
    contentHeight: Float,
    containerWidth: Float,
    containerHeight: Float,
): Float {
    if (contentWidth <= 0f || contentHeight <= 0f || containerWidth <= 0f || containerHeight <= 0f) return 0f
    return min(containerWidth / contentWidth, containerHeight / contentHeight)
}

/**
 * How far the preview may be dragged along one axis before its edge would leave the container: half
 * of whatever overflows it, or nothing while it still fits.
 */
internal fun maxPreviewOffset(fittedSize: Float, zoom: Float, containerSize: Float): Float =
    max(0f, (fittedSize * zoom - containerSize) / 2f)

/**
 * Applies one pinch/drag step. The zoom stays between fitted and [MAX_PREVIEW_ZOOM], and the offsets
 * are clamped so the preview can never be dragged out of view — which also means a fitted preview
 * does not move at all.
 */
internal fun PreviewTransform.transformedBy(
    zoomChange: Float,
    panX: Float,
    panY: Float,
    fittedWidth: Float,
    fittedHeight: Float,
    containerWidth: Float,
    containerHeight: Float,
): PreviewTransform {
    val newZoom = (zoom * zoomChange).coerceIn(MIN_PREVIEW_ZOOM, MAX_PREVIEW_ZOOM)
    val maxX = maxPreviewOffset(fittedWidth, newZoom, containerWidth)
    val maxY = maxPreviewOffset(fittedHeight, newZoom, containerHeight)
    return PreviewTransform(
        zoom = newZoom,
        offsetX = clampOffset(offsetX + panX, maxX),
        offsetY = clampOffset(offsetY + panY, maxY),
    )
}

/** An axis with no slack is pinned to exactly zero, so a fitted preview always equals the default. */
private fun clampOffset(offset: Float, max: Float): Float =
    if (max == 0f) 0f else offset.coerceIn(-max, max)

/** Double tap: zoom in from the fitted size, or back to it from any zoom. */
internal fun PreviewTransform.toggled(): PreviewTransform =
    if (isZoomed) PreviewTransform() else PreviewTransform(zoom = DOUBLE_TAP_PREVIEW_ZOOM)
