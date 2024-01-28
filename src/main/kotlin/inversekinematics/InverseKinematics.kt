package inversekinematics

import kotlin.math.*

/**
 * Solver using Cyclic Coordinate Descent algorithm to find the rotations
 * of arms to reach certain point.
 */
class InverseKinematics {

    /**
     * Tries to rotate bones/arms to reach the desired destination.
     * Bones cannot change their length, or disconnect from the chain,
     * but they can freely rotate.
     *
     * @param boneChain Bones to be rotated so that the final point is positioned at `target` if the arm is long
     *      enough to reach it. Chain is modified in place.
     * @param targetX Absolute X coordinate of the point that the chain will try to point to.
     * @param targetY Absolute Y coordinate of the point that the chain will try to point to.
     * @param precision How many iterations should be performed when trying to reach the destination point.
     *
     * @return Modified points from the `armPoints` parameter
     */
    fun rotateArmToReach(boneChain: BoneChain, targetX: Double, targetY: Double, precision: Int = 2) {
        for (i in 0..<precision) {
            updateBoneAngles(
                target = Point2D(targetX, targetY),
                boneChain = boneChain,
            )
        }
    }

    /**
     * Rotates each bone from the end so that an imaginary line from the current bone base
     * to the end of the whole bone chain points towards the target position.
     */
    private fun updateBoneAngles(target: Point2D, boneChain: BoneChain) {
        val targetVectors = getTargetVectors(
            target = target,
            boneChain = boneChain
        )

        var boneEnd = Point2D(boneChain.bones.last().length, 0.0)

        boneChain.bones.zip(targetVectors).reversed().forEach { (bone, targetVector) ->
            val degreeToVector = atan2(targetVector.y, targetVector.x)
            val degreeToBoneEnd = atan2(boneEnd.y, boneEnd.x)
            bone.relativeAngleRad += degreeToVector - degreeToBoneEnd

            boneEnd = boneEnd
                .rotate(bone.relativeAngleRad)
                .translate(x = bone.length)
        }
    }

    /**
     * Get a vector relative to each bone start, pointing in the direction of the target.
     */
    internal fun getTargetVectors(target: Point2D, boneChain: BoneChain): List<Point2D> = buildList {
        add(
            target
                .translate(-boneChain.absoluteStartX, -boneChain.absoluteStartY)
                .rotate(-boneChain.bones.first().relativeAngleRad)
        )

        boneChain.bones.zipWithNext().forEach { (previous, current) ->
            add(
                last()
                    // The X, Y only refer to the relative position compared to the other bone.
                    .translate(x = -previous.length)
                    .rotate(-current.relativeAngleRad)
            )
        }
    }

    internal data class Point2D(
        val x: Double,
        val y: Double
    ) {
        /**
         * @return (x, y) coordinates after rotating around point (0, 0) by a certain angle.
         *  (2, 0) rotated by PI/2 would be (0, 2).
         */
        fun rotate(angleRad: Double) = Point2D(
            x * cos(angleRad) - y * sin(angleRad),
            x * sin(angleRad) + y * cos(angleRad)
        )

        fun translate(x: Double = 0.0, y: Double = 0.0) = Point2D(this.x + x, this.y + y)
    }

}
