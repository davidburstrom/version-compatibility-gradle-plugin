package example

class KompatArrayUtils3Dot0 : KompatArrayUtils {
    override fun removeAllOccurrences(array: ByteArray, element: Byte): ByteArray {
        var count = 0
        for (b in array) {
            if (b == element) {
                count++
            }
        }
        val result = ByteArray(array.size - count)
        var j = 0
        for (b in array) {
            if (b != element) {
                result[j++] = b
            }
        }
        return result
    }
}
