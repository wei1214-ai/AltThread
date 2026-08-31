package com.example.myapplicationkoG.rendering

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Renderer abstraction. Part 1 uses [ComposeCanvasGarmentRenderer].
 * Part 2 / future work can drop in a RenderEffect or AGSL implementation
 * without touching the Editor.
 */
interface GarmentRenderer {
    fun draw(scope: DrawScope, state: RenderState)
}
