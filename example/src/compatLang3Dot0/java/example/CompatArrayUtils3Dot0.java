package example;

import org.checkerframework.checker.nullness.qual.NonNull;

public class CompatArrayUtils3Dot0 implements CompatArrayUtils {

  @Override
  public byte @NonNull [] removeAllOccurrences(final byte @NonNull [] array, final byte element) {
    int count = 0;
    for (final byte b : array) {
      if (b == element) {
        count++;
      }
    }
    byte[] result = new byte[array.length - count];
    int j = 0;
    for (final byte b : array) {
      if (b != element) {
        result[j++] = b;
      }
    }
    return result;
  }
}
