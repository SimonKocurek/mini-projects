package dominantSignalFrequency

import org.jtransforms.fft.DoubleFFT_1D
import java.time.Duration
import kotlin.math.cos

/**
 * Detect the dominant frequency from accelerometer data.
 *
 * This could be used to detect shaking of Parkinson patients.
 */
class DominantFrequencyDetector(
    private val discardSamplesOlderThan: Duration,
    private val minimumNeededSamples: Int = 10,
) {

    private data class Sample(
        val timestamp: Long,
        val x: Double,
        val y: Double,
        val z: Double,
    )

    private val samples = mutableListOf<Sample>()

    /**
     * Add a newest sample to the analysis.
     *
     * Also removes older samples.
     */
    fun addSample(x: Double, y: Double, z: Double, timestamp: Long) {
        samples.add(
            Sample(
                timestamp = timestamp,
                x = x,
                y = y,
                z = z,
            )
        )

        val oldSampleCutoff = timestamp - discardSamplesOlderThan.toMillis()
        samples.removeAll { it.timestamp < oldSampleCutoff }
    }

    /**
     * Gets the frequency of a sine wave that has the biggest magnitude.
     */
    fun getDominantFrequency(): Double? {
        val samplingRateHz = guessSamplingRateHz() ?: return null

        // We don't combine the `x`, `y`, and `z` data a
        // single value, to keep the original signal intact
        // (`sqrt(x**2)` is composed of different sine waves from `x`)
        val fftX = samples.map { it.x }.fourierTransform()
        val fftY = samples.map { it.y }.fourierTransform()
        val fftZ = samples.map { it.z }.fourierTransform()

        // Out of the decomposed sine waves, we want to pick the
        // one that has the highest magnitude:
        val indexOfSineWaveWithMaxMagnitude = findMaxMagnitudeIndex(fftX, fftY, fftZ)
        val sineWaveToFrequency = samplingRateHz / fftX.size
        return indexOfSineWaveWithMaxMagnitude * sineWaveToFrequency
    }

    // FFT relies on uniform time between samples.
    // Our samples are not guaranteed to be recorded in
    // uniform time intervals. So we will assume that
    // over time, we will receive samples in roughly
    // same time intervals. And rely on average time
    // difference as the sampling rate.
    //
    // If it turns out we receive data in very randomized
    // time intervals. Then this guess will need to be
    // replaced by updating the samples to fit the assumption.
    private fun guessSamplingRateHz(): Double? {
        if (samples.size < minimumNeededSamples) {
            return null
        }

        val totalDurationSec = (samples.last().timestamp - samples.first().timestamp) / 1000.0
        if (totalDurationSec == 0.0) {
            return null // Avoid division by 0
        }

        return (samples.size - 1) / totalDurationSec
    }

    private fun List<Double>.fourierTransform(): DoubleArray {
        // Since we are doing the analysis only on a small window
        // from all possible samples over all the time, applying a
        // window function might help prevent spectral leakage.
        // This function tapers off the samples at the edges of the window.
        val samples = hammingWindow()

        // Break the signal into sine waves in-place:
        val fft = DoubleFFT_1D(samples.size.toLong())
        fft.realForward(samples)
        return samples
    }

    private fun List<Double>.hammingWindow() = DoubleArray(size) { i ->
        val window = 0.54 - 0.46 * cos(2 * Math.PI * i / (size - 1))
        window * get(i)
    }

    private fun findMaxMagnitudeIndex(
        fftXData: DoubleArray,
        fftYData: DoubleArray,
        fftZData: DoubleArray,
    ): Int {
        val magnitudesSquared = DoubleArray(fftXData.size / 2) { i ->
            // Technically, there should also be a square root here,
            // but it wouldn't affect the result of `maxBy` operation.
            // So we can skip the operation.
            fftXData.magnitudeAt(i) + fftYData.magnitudeAt(i) + fftZData.magnitudeAt(i)
        }

        return magnitudesSquared.indices.drop(1).maxBy { magnitudesSquared[it] }
    }

    private fun DoubleArray.magnitudeAt(i: Int): Double {
        val realPart = this[2 * i]
        val imaginaryPart = this[2 * i + 1]

        return (realPart * realPart) + (imaginaryPart * imaginaryPart)
    }
}
