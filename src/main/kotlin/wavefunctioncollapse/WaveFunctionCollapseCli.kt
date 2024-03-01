package wavefunctioncollapse

import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    WaveFunctionCollapse().main(args)
}

class WaveFunctionCollapse: CliktCommand(
    name = "wavefunctioncollapse",
) {

    private val kernelSize = 3

    override fun run(): Unit = runBlocking(Dispatchers.Default) {
        val image = withContext(Dispatchers.IO) { ImageIO.read(File("Test.png")) }

        for (y in 0..image.height - kernelSize) {
            for (x in 0..image.width - kernelSize) {
                image.getSubimage(x, y, kernelSize, kernelSize)

                // original
                // Rotate 90
                // Rotate 180
                // Rotate 270
                // horizontal flip
                // vertical flip

            }
        }

        // Find unique patterns, their count and Assign IDs

        // Find overlaps
        // top 6
        // bottom 6
        // left 6
        // right 6

    }

}
