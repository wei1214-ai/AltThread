package com.example.myapplicationkoG.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/**
 * A vector path in GARMENT coordinates. Points are stored as-is; smoothing
 * happens when the renderer draws them, not when the user lifts the finger.
 *
 * Smoothing rule: Catmull-Rom over the points with tension 0.5, producing a
 * cubic Bezier segment between every pair of consecutive points. Stable,
 * GPU-friendly, no per-frame allocation.
 */
@Serializable
data class VectorPath(
    val points: List<Point>,
    val closed: Boolean = false
) {
    val isEmpty: Boolean get() = points.isEmpty()
    val size: Int get() = points.size

    fun append(point: Point, minStep: Float = 1.5f): VectorPath {
        val last = points.lastOrNull()
        if (last != null && last.minus(point).length() < minStep) return this
        return copy(points = points + point)
    }

    /**
     * Catmull-Rom -> Bezier conversion. Returns a list of cubic Bezier
     * control points (4 per segment). Output is in the same coordinate
     * system as the input. Empty / single-point paths return empty.
     */
    fun toBezierSegments(tension: Float = 0.5f): List<FloatArray> {
        val pts = points
        if (pts.size < 2) return emptyList()
        val segments = mutableListOf<FloatArray>()
        val n = pts.size
        for (i in 0 until n - 1) {
            val p0 = pts[if (i == 0) 0 else i - 1]
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p3 = pts[if (i + 2 < n) i + 2 else i + 1]

            val c1x = p1.x + (p2.x - p0.x) * (tension / 3f)
            val c1y = p1.y + (p2.y - p0.y) * (tension / 3f)
            val c2x = p2.x - (p3.x - p1.x) * (tension / 3f)
            val c2y = p2.y - (p3.y - p1.y) * (tension / 3f)

            segments += floatArrayOf(
                p1.x, p1.y,
                c1x, c1y,
                c2x, c2y,
                p2.x, p2.y
            )
        }
        return segments
    }

    companion object {
        fun empty() = VectorPath(emptyList(), false)
    }
}
