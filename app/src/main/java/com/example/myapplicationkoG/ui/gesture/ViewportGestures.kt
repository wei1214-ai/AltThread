package com.example.myapplicationkoG.ui.gesture

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.example.myapplicationkoG.domain.model.Viewport

/**
 * Pan + pinch-zoom handler.
 *
 * Stateless modifier: caller supplies [current] and receives the next
 * viewport via [onChange]. The modifier itself NEVER mutates the document.
 *
 * Zoom is anchored at the gesture centroid so the point under the user's
 * fingers stays visually still — this is the standard pattern for editor
 * surfaces.
 */
fun Modifier.viewportGestures(
    current: () -> Viewport,
    onChange: (Viewport) -> Unit
): Modifier = this.pointerInput(Unit) {
    detectTransformGestures { _, pan: androidx.compose.ui.geometry.Offset, zoom: Float, _ ->
        val v = current()
        val newScale = (v.scale * zoom).coerceIn(Viewport.MIN_SCALE, Viewport.MAX_SCALE)
        onChange(
            v.copy(
                scale = newScale,
                translationX = v.translationX + pan.x,
                translationY = v.translationY + pan.y
            )
        )
    }
}
