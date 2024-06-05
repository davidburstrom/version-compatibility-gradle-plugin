package example

import org.apache.commons.lang3.ArrayUtils

class KompatArrayUtils3Dot5 : KompatArrayUtils {
    override fun removeAllOccurrences(array: ByteArray, element: Byte): ByteArray = ArrayUtils.removeAllOccurences(array, element)
}
