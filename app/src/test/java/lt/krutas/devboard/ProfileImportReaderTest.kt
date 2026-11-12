package lt.krutas.devboard

import java.io.Reader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfileImportReaderTest {
    @Test fun readsUntilEndEvenWhenReaderReturnsShortChunks() {
        val input = "{\"profiles\":{\"Default\":{}}}"
        val partial = object : Reader() {
            var index = 0
            override fun read(buffer: CharArray, offset: Int, length: Int): Int {
                if (index == input.length) return -1
                val count = minOf(3, length, input.length - index)
                input.toCharArray(buffer, offset, index, index + count)
                index += count
                return count
            }
            override fun close() {}
        }
        assertEquals(input, ProfileImportReader.read(partial, 256_000))
    }

    @Test fun rejectsInputBeyondLimitInsteadOfImportingTruncatedData() {
        assertThrows(IllegalArgumentException::class.java) {
            ProfileImportReader.read("123456".reader(), 5)
        }
        assertEquals("12345", ProfileImportReader.read("12345".reader(), 5))
    }
}
