package com.qhathia.mobile.staff.core.ui.component.productpreview

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize

/**
 * Shows [content] the way an image viewer shows a picture.
 *
 * The content is laid out at its fixed [designSize] — ignoring the font scale, so it is the same
 * drawing on every device — then scaled to fit the canvas. The merchant can pinch to zoom in, drag
 * while zoomed and double-tap to toggle the zoom; the transform starts over whenever [resetKey] or
 * the canvas size (e.g. on rotation) changes.
 *
 * Nothing inside [content] is interactive: a layer above it takes every pointer event, and the whole
 * subtree is announced as a single image described by [contentDescription].
 */
@Composable
internal fun ProductPreviewCanvas(
    designSize: DpSize,
    contentDescription: String,
    resetKey: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Image
            },
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val designWidth = with(density) { designSize.width.toPx() }
        val designHeight = with(density) { designSize.height.toPx() }
        val fit = fitScale(designWidth, designHeight, containerWidth, containerHeight)

        var transform by remember(resetKey, containerWidth, containerHeight) {
            mutableStateOf(PreviewTransform())
        }

        Box(
            modifier = Modifier
                .requiredSize(designSize)
                .graphicsLayer {
                    scaleX = fit * transform.zoom
                    scaleY = fit * transform.zoom
                    translationX = transform.offsetX
                    translationY = transform.offsetY
                }
                .clearAndSetSemantics { },
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = 1f),
                content = content,
            )
        }

        // Sits above the content so the Discovery controls drawn in it never receive a tap.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(fit, containerWidth, containerHeight) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        transform = transform.transformedBy(
                            zoomChange = zoom,
                            panX = pan.x,
                            panY = pan.y,
                            fittedWidth = designWidth * fit,
                            fittedHeight = designHeight * fit,
                            containerWidth = containerWidth,
                            containerHeight = containerHeight,
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { transform = transform.toggled() })
                },
        )
    }
}
