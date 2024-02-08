package inversekinematics

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal class InverseKinematicsGuiPaintPanel(
    private val inverseKinematics: InverseKinematics,
    private val bufferedImage: BufferedImage,
    private val boneChain: BoneChain
) : JPanel(), MouseMotionListener {

    private val targetFrameMs = 16L

    init {
        addMouseMotionListener(this)
        repaintBoneChain()
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        graphics.drawImage(bufferedImage, 0, 0, this)
    }

    override fun mouseDragged(e: MouseEvent) = onMouseEvent(e)

    override fun mouseMoved(e: MouseEvent) = onMouseEvent(e)

    private fun onMouseEvent(e: MouseEvent) {
        inverseKinematics.rotateArmToReach(
            boneChain = boneChain,
            targetX = e.x.toDouble(),
            targetY = e.y.toDouble(),
        )
        repaintBoneChain()
    }

    private fun repaintBoneChain() {
        val graphics = bufferedImage.graphics as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        graphics.clearRect(0, 0, width, height)

        toPoints(boneChain).reversed().zipWithNext().forEachIndexed { index, (current, next) ->
            graphics.color = Color.getHSBColor(index / 25.0f, 0.5f, 1.0f)
            graphics.stroke = BasicStroke(3f + index * 1.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)

            graphics.drawLine(
                current.first,
                current.second,
                next.first,
                next.second
            )
        }
        graphics.dispose()

        repaint(targetFrameMs)
    }

    /**
     * Converts series of connected lines to 2D points.
     */
    private fun toPoints(boneChain: BoneChain): List<Pair<Int, Int>> = buildList {
        var lastAbsoluteX = boneChain.absoluteStartX
        var lastAbsoluteY = boneChain.absoluteStartY
        var lastAbsoluteAngle = 0.0

        add(
            Pair(
                lastAbsoluteX.roundToInt(),
                lastAbsoluteY.roundToInt(),
            )
        )

        boneChain.bones.forEach { bone ->
            lastAbsoluteAngle += bone.relativeAngleRad

            val newAbsoluteX = lastAbsoluteX + cos(lastAbsoluteAngle) * bone.length
            val newAbsoluteY = lastAbsoluteY + sin(lastAbsoluteAngle) * bone.length

            lastAbsoluteX = newAbsoluteX
            lastAbsoluteY = newAbsoluteY

            add(
                Pair(
                    newAbsoluteX.roundToInt(),
                    newAbsoluteY.roundToInt(),
                )
            )
        }
    }
}
