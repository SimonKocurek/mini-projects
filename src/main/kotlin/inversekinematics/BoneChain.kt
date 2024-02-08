package inversekinematics

internal data class BoneChain(
    val absoluteStartX: Double,
    val absoluteStartY: Double,
    val bones: List<Bone>,
)

internal data class Bone(
    // Angle is relative to the parent bone.
    // Relative value is useful, since we are updating from end
    // and therefore we don't need to iterate over other bones
    // to change the angle.
    var relativeAngleRad: Double,
    val length: Double,
)
