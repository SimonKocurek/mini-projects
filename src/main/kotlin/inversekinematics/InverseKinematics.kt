package inversekinematics

import java.awt.Canvas
import java.awt.GraphicsConfiguration
import javax.swing.JFrame

fun main(args: Array<String>) {
    JFrame("Inverse Kinematics Demo").apply {
        add(Canvas().apply {
            setSize(800, 800)
        })
        pack()
        isVisible = true
    }
}

class InverseKinematics {

}