package example;

import org.apache.commons.lang3.ArrayUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CompatArrayUtils3Dot5 implements CompatArrayUtils {

  @Override
  public byte[] removeAllOccurrences(final byte @NonNull [] array, final byte element) {
    return ArrayUtils.removeAllOccurences(array, element);
  }
}
