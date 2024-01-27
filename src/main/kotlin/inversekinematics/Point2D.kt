package inversekinematics

import kotlin.math.atan2
import kotlin.math.sqrt

data class Point2D(
    var x: Int,
    var y: Int,
) {

    /**
     * √((x2 − x1)^2 + (y2 − y1)^2)
     */
    fun distanceTo(b: Point2D)= sqrt(
        (x - b.x) * (x - b.x) + (y - b.y) * (y - b.y).toDouble()
    )

    fun radAngleTo(b: Point2D) = atan2(
        b.y - y.toDouble(),
        b.x - x.toDouble()
    )
}
