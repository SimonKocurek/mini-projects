package inversekinematics

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

private const val X_SIZE = 600
private const val Y_SIZE = 600

fun main(args: Array<String>) {
    SwingUtilities.invokeLater {
        // Inverse kinematics is used for figuring out how an arm consisting of many
        // segments/bones/joints should rotate so that the final point reaches the
        // desired position.
        val armPoints = (0..6).map { Point2D(X_SIZE / 2, Y_SIZE - it * 75) }
        val inverseKinematics = InverseKinematics()
        InverseKinematicsGui(inverseKinematics, armPoints)
    }
}

class InverseKinematicsGui(
    inverseKinematics: InverseKinematics,
    armPoints: List<Point2D>
) : JFrame("Inverse Kinematics Demo") {

    init {
        setSize(X_SIZE, Y_SIZE)
        setLocationRelativeTo(null)
        isResizable = false
        defaultCloseOperation = EXIT_ON_CLOSE

        val paintPanel = InverseKinematicsPaintPanel(
            inverseKinematics = inverseKinematics,
            bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB),
            armPoints = armPoints
        )
        contentPane.add(paintPanel)

        isVisible = true
    }
}

private class InverseKinematicsPaintPanel(
    private val inverseKinematics: InverseKinematics,
    private val bufferedImage: BufferedImage,
    private var armPoints: List<Point2D>
) : JPanel(), MouseMotionListener {

    private val targetFrameMs = 16L

    init {
        addMouseMotionListener(this)
        repaintArm()
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        graphics.drawImage(bufferedImage, 0, 0, this)
    }

    override fun mouseDragged(e: MouseEvent) = onMouseEvent(e)

    override fun mouseMoved(e: MouseEvent) = onMouseEvent(e)

    private fun onMouseEvent(e: MouseEvent) {
        armPoints = inverseKinematics.rotateArmToReach(
            armPoints = armPoints,
            destination = Point2D(x = e.x, y = e.y)
        )
        repaintArm()
    }

    private fun repaintArm() {
        val graphics = bufferedImage.graphics as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        graphics.clearRect(0, 0, width, height)
        armPoints.zipWithNext().reversed().forEachIndexed { index, (current, next) ->
            graphics.color = Color.getHSBColor(index / 25.0f, 0.5f, 1.0f)
            graphics.stroke = BasicStroke(3f + index * 1.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)

            graphics.drawLine(
                current.x,
                current.y,
                next.x,
                next.y
            )
        }
        graphics.dispose()

        repaint(targetFrameMs)
    }

}