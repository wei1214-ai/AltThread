package com.example.myapplicationkoG.domain.model

import kotlinx.serialization.Serializable

/**
 * 2D point stored in GARMENT coordinates (i.e. the source image's native
 * resolution), NEVER screen or viewport coordinates. Viewport transforms
 * (scale + translation) are applied at draw time only.
 */
@Serializable
data class Point(val x: Float, val y: Float) {
    operator fun plus(o: Point) = Point(x + o.x, y + o.y)
    operator fun minus(o: Point) = Point(x - o.x, y - o.y)
    operator fun times(s: Float) = Point(x * s, y * s)
    fun length(): Float = kotlin.math.sqrt(x * x + y * y)
}
