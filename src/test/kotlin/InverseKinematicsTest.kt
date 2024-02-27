import inversekinematics.Bone
import inversekinematics.BoneChain
import inversekinematics.InverseKinematics
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals

class InverseKinematicsTest {

    @Test
    fun canCalculateRelativeTargetVectors() {
        // Given
        val inverseKinematics = InverseKinematics()

        // When
        val relativeTargets = inverseKinematics.getTargetVectors(
            target = InverseKinematics.Point2D(300.0, 50.0),
            boneChain = BoneChain(
                absoluteStartX = 200.0,
                absoluteStartY = 200.0,
                bones = listOf(
                    Bone(0.0, 100.0),
                    Bone(2.0, 50.0),
                    Bone(-1.0, 75.0),
                )
            )
        )

        // Then
        assertEquals(
            listOf(
                InverseKinematics.Point2D(100.0, -150.0),
                InverseKinematics.Point2D(-136.39, 62.42),
                InverseKinematics.Point2D(-153.24, -123.12),
            ),
            relativeTargets.map {
                InverseKinematics.Point2D(
                    (it.x * 100).roundToInt() / 100.0,
                    (it.y * 100).roundToInt() / 100.0
                )
            }
        )
    }
}