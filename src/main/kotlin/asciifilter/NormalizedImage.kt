package asciifilter

internal data class NormalizedImage(
    val brightness: List<List<Int>>
) {
    val width = brightness[0].size
    val height = brightness.size
}
