package utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintStream

fun DeferContext.captureStreams(
    newStdIn: InputStream = ByteArrayInputStream(ByteArray(1000)),
): Pair<ByteArrayOutputStream, ByteArrayOutputStream> {
    val stdIn = System.`in`
    val stdOut = System.out
    val stdErr = System.err

    val newStdOut = ByteArrayOutputStream(1000)
    val newStdOutStream = PrintStream(newStdOut)
    defer { newStdOutStream.close() }
    System.setOut(newStdOutStream)
    defer { System.setOut(stdOut) }

    val newStdErr = ByteArrayOutputStream(1000)
    val newStdErrStream = PrintStream(newStdErr)
    defer { newStdErrStream.close() }
    System.setErr(newStdErrStream)
    defer { System.setErr(stdErr) }

    System.setIn(newStdIn)
    defer { System.setIn(stdIn) }

    return Pair(newStdOut, newStdErr)
}
