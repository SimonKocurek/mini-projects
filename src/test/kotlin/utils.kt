import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream


fun captureStreams(action: (stdIn: ByteArrayInputStream, stdOut: ByteArrayOutputStream, stdErr: ByteArrayOutputStream) -> Unit) {
    val stdIn = System.`in`
    val stdOut = System.out
    val stdErr = System.err

    try {
        ByteArrayInputStream(ByteArray(1000)).use { newStdIn ->
            ByteArrayOutputStream(1000).use { newStdOut ->
                PrintStream(newStdOut).use { newStdOutStream ->
                    ByteArrayOutputStream(1000).use { newStdErr ->
                        PrintStream(newStdErr).use { newStdErrStream ->
                            System.setIn(newStdIn)
                            System.setOut(newStdOutStream)
                            System.setErr(newStdErrStream)

                            action(newStdIn, newStdOut, newStdErr)
                        }
                    }
                }
            }
        }
    } finally {
        System.setErr(stdErr)
        System.setOut(stdOut)
        System.setIn(stdIn)
    }
}