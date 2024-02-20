package asciifilter

import asciifilter.character.AlphabetBrightnessNormalizer
import asciifilter.character.AlphabetSorter
import asciifilter.character.AsciiFilterAlphabetFactory
import asciifilter.character.CharacterBitmapper
import asciifilter.image.AsciiArtImageFactory
import asciifilter.image.ImageBrightnessNormalizer
import asciifilter.image.ImageRescaler
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.*
import utils.LoggerFactory
import java.io.File
import javax.imageio.ImageIO


fun main(args: Array<String>) {
    AsciiFilterCli().main(args)
}

internal class AsciiFilterCli : CliktCommand(
    name = "asciifilter",
//    help = """
//        Cut out selected portions of each line of a file.
//
//        Outputs result in format "<line1 field1><delimiter><line1 field2>\n<line2 field1><delimiter><line2 field2>\n"
//
//        > If enough delimited fields are not found in the line, whole line is printed out.
//
//        Examples:
//        ```bash
//        ${'$'} cut -d : -f 1,7 /etc/passwd
//        nobody:/usr/bin/false
//        root:/bin/sh
//        ```
//        ```bash
//        ${'$'} who | cut -f 6 -d ' '
//        console
//        ttys000
//        ```
//    """.trimIndent(),
//    printHelpOnEmptyArgs = true
) {

//    private val filePath by argument(
//        name = "file",
//        help = "A pathname of an input file. If no file operands are specified, the standard input shall be used."
//    ).path(mustExist = true, canBeDir = false, mustBeReadable = true).optional()

    private val targetWidth by option(
        "-w",
        "--width",
        help = "The number of character width of the result image. Default: 75"
    ).int().default(40)


    // Alphabet - does not need to be ASCII. Should be monospace. No sorting needed.
    //
    // $@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\|()1{}[]?-_+~<>i!lI;:,"^`'.
//     val alphabet = "כלאדםזכאילזכויותולחרויותשנקבעובהכרזשזוללאהפליהכלשהיא"
    val alphabet = "인권에대한무시와경멸이인류의양심을격분시키는만행을초래하였으며인간이언론과신앙의자유그리고공포와저나는한국의문화를좋아합니다자연은름워요산과강그리고바가있어벚꽃도정말답습음식맛김치와불기라면등이공부하ㄱㄴㄷㄹㅁㅏㅓㅗㅜㅡ".toList().distinct().joinToString("")
//    val alphabet = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん【】『』、。！"
//    val alphabet = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン【】『』、。！"
//    val alphabet = "私わたしは日本にほんの文化ぶかが好すきで！自然ぜ美うつくい。山やま川、そて海みあり桜さら花なもとれ食べ物お寿司刺身ラーメンど語ご勉強ょ【】『』"
//    val alphabet = "龘齉龖龥龎漢隶䶻鳽籥鱻林山江河湖海花草树鸟兽鱼虫蝶蜂蚁螺龟蛇狗猫猴熊狼狮象犀鸭鹅鹤鹰鸽鸦木火水石土金银天日月星云风雨雪芽枝兔猪牛马羊豬龙飞船人口手目耳田女男子犬《》"
//    val alphabet = "!\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ "
//    val alphabet = "АаБбВвГгДдЕеËëЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЪъЫыЬьЭэЮюЯя\\\"#\\\$%&'()*+,"
//     val alphabet = "ΑαΒβΓγΔδΕεΖζΗηΘθΙιΚκΛλΜμΝνΞξΟοΠπΡρΣςσΤτΥυΦφΧχΨψΩω.;',;=:!-“”"
//    val alphabet = " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
//    val alphabet = ".:-=+*#%@ "

    // file
    val file = File("m.jpg")

    private val verbose by option(
        "-v",
        "--verbose",
        help = "Enable debug logging."
    ).flag()

    private val fontSize by option(
        "--fontSize",
        help = "The font size that will be used to calculate how bright or dark each character is. " +
                "Lower values will lead to faster startup and lower memory usage. " +
                "However, lowering this too much will result in worse ASCII image quality. " +
                "Default: 10"
    ).int().default(10)

    override fun run() = runBlocking(Dispatchers.Default) {
        // Dependencies
        val logger = LoggerFactory.create("AsciiFilter", enabled = verbose)
        val asciiFilterAlphabetFactory = AsciiFilterAlphabetFactory(
            bitmapper = CharacterBitmapper(logger),
            alphabetSorter = AlphabetSorter(),
            normalizer = AlphabetBrightnessNormalizer()
        )
        val asciiArtImageFactory = AsciiArtImageFactory(
            logger = logger,
            rescaler = ImageRescaler(),
            normalizer = ImageBrightnessNormalizer(logger)
        )
        val filter = AsciiFilter(logger)

        // Processing
        val alphabet = asciiFilterAlphabetFactory.create(alphabet, fontSize)
        val image = asciiArtImageFactory.prepare(file, targetWidth, alphabet.averageAspectRatio)
        val asciiImage = filter.convert(image, alphabet.characters)

        // Output
        println(asciiImage)
    }

}
