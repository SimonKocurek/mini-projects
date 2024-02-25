import asciifilter.AsciiFilterCli
import org.junit.jupiter.api.Test
import utils.*
import kotlin.io.path.absolutePathString
import kotlin.math.min
import kotlin.test.assertEquals

class AsciiFilterTest {

    @Test
    fun canConvertPngWithTransparency() = withDefer {
        // Given
        val imageFile = withImageFile(
            colors = (0..255).map { y ->
                (0..255).map { x ->
                    Pixel(a=x, r=0, g=0, b=0)
                }
            },
            suffix = "png"
        )
        val (stdOut, stdErr) = captureStreams()

        // When
        AsciiFilterCli().main(listOf(
            "--alphabet", ".:-=+*#%@",
            "--width", "20",
            imageFile.absolutePathString()
        ))

        // Then
        assertEquals("""
            .-::++==***%%%####@@
            .-::++==***%%%####@@
            .-::++==***%%%####@@
            .-::++==***%%%####@@
            .-::++==***%%%####@@
            .-::++==***%%%####@@
            .-::++==***%%%####@@
            .-::++==***%%%####@@
            .-::++==***%%%####@@
            
        """.trimIndent(), stdOut.toString())
        assertEquals("", stdErr.toString())
    }

    @Test
    fun canConvertJpgGradient() = withDefer {
        // Given
        val imageFile = withImageFile(
            colors = (0..255).map { y ->
                (0..255).map { x ->
                    Pixel(r=y, g=(x + y) / 2, b=y)
                }
            },
            suffix = "jpg"
        )
        val (stdOut, stdErr) = captureStreams()

        // When
        AsciiFilterCli().main(listOf(
            "--alphabet", "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん【】『』、。！",
            "--width", "10",
            imageFile.absolutePathString()
        ))

        // Then
        assertEquals("""
            ぬほほねゆはきをわれ
            ねはきをなれむひもす
            なのむかそたらよん】
            けやらよん】うとてこ
            ん】うとていししへ』
            ていししへ』『『『！
            へく『『『！！！！。
            『『！！！。。。、、

        """.trimIndent(), stdOut.toString())
        assertEquals("", stdErr.toString())
    }

    @Test
    fun canConvertGif() = withDefer {
        // Given
        val imageFile = withImageFile(
            colors = (0..255).map { y ->
                (0..255).map { x ->
                    Pixel(r=(x + y) / 2, g=(x + y) / 2, b=y)
                }
            },
            suffix = "gif"
        )
        val (stdOut, stdErr) = captureStreams()

        // When
        AsciiFilterCli().main(listOf(
            "--width", "20",
            imageFile.absolutePathString()
        ))

        // Then
        assertEquals("""
            @@@@W@@@W@@QQQQQQQQd
            W@W0@0@dQdQddddhdhdh
            Q#dOdOddXdXdId*h*h*h
            ddXdXh%IhICI*******<
            hhhdh*****{*|t<<|~^<
            **h***{*t<t|<~|~~~~~
            <*<<<<<t~r~r~~~~-:-~
            <<<<<~~~~~:~-.-`````
            ~~~~''''''''``````` 

        """.trimIndent(), stdOut.toString())
        assertEquals("", stdErr.toString())
    }
}
