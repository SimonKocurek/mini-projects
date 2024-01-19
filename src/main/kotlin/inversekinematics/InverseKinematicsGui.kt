package inversekinematics

import java.awt.Graphics
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    SwingUtilities.invokeLater {
        val ik = InverseKinematics()
        InverseKinematicsGui(ik)
    }
}

class InverseKinematicsGui(
    inverseKinematics: InverseKinematics
) : JFrame("Inverse Kinematics Demo") {

    init {
        setSize(600, 600)
        setLocationRelativeTo(null)
        isResizable = false
        defaultCloseOperation = EXIT_ON_CLOSE

        val paintPanel = InverseKinematicsPaintPanel(
            inverseKinematics = inverseKinematics,
            bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        )
        contentPane.add(paintPanel)

        isVisible = true
    }
}

private class InverseKinematicsPaintPanel(
    private val inverseKinematics: InverseKinematics,
    private val bufferedImage: BufferedImage
) : JPanel(), MouseMotionListener {

    private val TARGET_FRAME_MS = 16L

    private var lastX = 0
    private var lastY = 0

    init {
        addMouseMotionListener(this)
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        graphics.drawImage(bufferedImage, 0, 0, this)
    }

    override fun mouseDragged(e: MouseEvent) {
        val graphics = bufferedImage.createGraphics()
        graphics.drawLine(lastX, lastY, e.x, e.y)
        lastX = e.x
        lastY = e.y
        graphics.dispose()

        repaint(TARGET_FRAME_MS)
    }

    override fun mouseMoved(e: MouseEvent) {

    }

}