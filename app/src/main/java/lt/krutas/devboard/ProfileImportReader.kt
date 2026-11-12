package lt.krutas.devboard

import java.io.Reader

internal object ProfileImportReader {
    fun read(reader: Reader, limit: Int): String {
        val output = StringBuilder()
        val buffer = CharArray(4096)
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            require(output.length + count <= limit) { "JSON file exceeds 256 KB" }
            output.append(buffer, 0, count)
        }
        return output.toString()
    }
}
