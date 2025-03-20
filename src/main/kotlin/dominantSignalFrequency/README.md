# Dominant Signal Frequency

This can detect user (possibly with Parkinson's disease) shaking their fitness watch, 
by analyzing the dominant frequency of the accelerometer data.

## Example usage

```kotlin
val dominantFrequencyDetector = DominantFrequencyDetector(
    discardSamplesOlderThan = Duration.ofSeconds(3)
)
val debouncer = Debouncer<Boolean>(debounceDuration = Duration.ofMillis(100))

fun onSensorChanged(accelometerEvent: AccelometerEvent) {
    dominantFrequencyDetector.addSample(
        x = accelometerEvent.x,
        y = accelometerEvent.y,
        z = accelometerEvent.z,
        timestamp = System.currentTimeMillis()
    )

    val dominantShakingFrequency = dominantFrequencyDetector.getDominantFrequency() ?: -1.0
    val shakingDetected = debouncer.debounce(dominantShakingFrequency in TREMOR_RANGE)
    
    // Handle shaking
}
```
