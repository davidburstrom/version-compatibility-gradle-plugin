package example;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface CompatArrayUtils {

  byte[] removeAllOccurrences(byte @NonNull [] array, byte element);
}
