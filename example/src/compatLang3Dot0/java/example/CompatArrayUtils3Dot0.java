/*
 * Copyright 2022-2023 David Burström
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
