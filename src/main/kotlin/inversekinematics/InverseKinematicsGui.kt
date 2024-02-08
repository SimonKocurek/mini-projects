package inversekinematics

import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.SwingUtilities

private const val X_SIZE = 600
private const val Y_SIZE = 600

fun main(args: Array<String>) {
    SwingUtilities.invokeLater {
        // Inverse kinematics is used for figuring out how an arm consisting of many
        // bones should rotate so that the final point reaches the desired position.
        val boneChain = BoneChain(
            absoluteStartY = Y_SIZE.toDouble(),
            absoluteStartX = X_SIZE / 2.0,
            bones = (0..6).map { Bone(-0.35, 75.0) }
        )
        val inverseKinematics = InverseKinematics()
        InverseKinematicsGui(inverseKinematics, boneChain)
    }
}

private class InverseKinematicsGui(
    inverseKinematics: InverseKinematics,
    boneChain: BoneChain
) : JFrame("Inverse Kinematics Demo") {

    init {
        setSize(X_SIZE, Y_SIZE)
        setLocationRelativeTo(null)
        isResizable = false
        defaultCloseOperation = EXIT_ON_CLOSE

        val paintPanel = InverseKinematicsGuiPaintPanel(
            inverseKinematics = inverseKinematics,
            bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB),
            boneChain = boneChain
        )
        contentPane.add(paintPanel)

        isVisible = true
    }
}
