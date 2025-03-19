import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import dominantSignalFrequency.DominantFrequencyDetector
import java.time.Duration
import kotlin.math.PI
import kotlin.math.sin

class DominantFrequencyDetectorTest {

    @Test
    fun `test single sine wave detection`() {
        val detector = DominantFrequencyDetector(
            discardSamplesOlderThan = Duration.ofSeconds(15),
        )
        val frequency = 6.0
        generateSineWave(
            frequency = frequency,
            magnitude = 10.0,
        ).forEach {
            detector.addSample(
                timestamp = it.timestamp,
                x = it.acceleration,
                y = 0.0,
                z = 0.0,
            )
        }

        val detectedFrequency = detector.getDominantFrequency()

        assertEquals(frequency, detectedFrequency!!, 0.2)
    }

    @Test
    fun `test multiple sine waves detection`() {
        val detector = DominantFrequencyDetector(
            discardSamplesOlderThan = Duration.ofSeconds(15),
        )
        val highestMagnitude = 20.0
        val frequencyAtHighestMagnitude = 5.0
        val xSine = generateSineWave(
            frequency = 2.0,
            magnitude = highestMagnitude / 2,
        )
        val ySine = generateSineWave(
            frequency = frequencyAtHighestMagnitude,
            magnitude = highestMagnitude,
        )
        val zSine = generateSineWave(
            frequency = 2.0,
            magnitude = highestMagnitude / 4,
        )
        xSine.indices.forEach { i ->
            detector.addSample(
                timestamp = xSine[i].timestamp,
                x = xSine[i].acceleration,
                y = ySine[i].acceleration,
                z = zSine[i].acceleration,
            )
        }

        val detectedFrequency = detector.getDominantFrequency()

        assertEquals(frequencyAtHighestMagnitude, detectedFrequency!!, 0.2)
    }

    @Test
    fun `test multiple sine waves combined in a single direction detection`() {
        val detector = DominantFrequencyDetector(
            discardSamplesOlderThan = Duration.ofSeconds(15),
        )
        val highestMagnitude = 50.0
        val frequencyAtHighestMagnitude = 5.0
        val lowerSine = generateSineWave(
            frequency = 2.0,
            magnitude = highestMagnitude / 8,
        )
        val dominantSine = generateSineWave(
            frequency = frequencyAtHighestMagnitude,
            magnitude = highestMagnitude,
        )
        val lowestSine = generateSineWave(
            frequency = 2.0,
            magnitude = highestMagnitude / 25,
        )
        lowerSine.indices.forEach { i ->
            detector.addSample(
                timestamp = lowerSine[i].timestamp,
                x = lowerSine[i].acceleration + dominantSine[i].acceleration + lowestSine[i].acceleration,
                y = 0.0,
                z = 0.0,
            )
        }

        val detectedFrequency = detector.getDominantFrequency()

        assertEquals(frequencyAtHighestMagnitude, detectedFrequency!!, 0.2)
    }

    private data class SignalPoint(val acceleration: Double, val timestamp: Long)

    private fun generateSineWave(
        frequency: Double,
        magnitude: Double,
    ): List<SignalPoint> {
        val sampleRateHz = 15.0
        val sampleDuration = Duration.ofSeconds(20)

        val numSamples = (sampleRateHz * sampleDuration.seconds).toInt()
        val samples = mutableListOf<SignalPoint>()

        for (i in 0..<numSamples) {
            val time = i / sampleRateHz
            val value = magnitude * sin(2 * PI * frequency * time)
            samples.add(
                SignalPoint(
                    acceleration = value,
                    timestamp = (time * 1000).toLong()
                )
            )
        }

        return samples
    }

}
