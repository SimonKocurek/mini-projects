package wavefunctioncollapse

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    WaveFunctionCollapse().main(args)
}

class WaveFunctionCollapse: CliktCommand(
    name = "wavefunctioncollapse",
) {
    override fun run() {
        ImageIO.read(File("Test.png"))
    }

}
