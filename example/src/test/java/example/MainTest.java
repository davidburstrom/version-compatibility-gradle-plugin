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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MainTest {

  @Test
  void testJava() {
    Assertions.assertArrayEquals(
        new byte[] {}, Main.removeAllOccurrences(new byte[] {1}, (byte) 1));
    Assertions.assertArrayEquals(
        new byte[] {2}, Main.removeAllOccurrences(new byte[] {2}, (byte) 1));
  }

  @Test
  void testKotlin() {
    Assertions.assertArrayEquals(
        new byte[] {}, Main.removeAllOccurrencesKotlin(new byte[] {1}, (byte) 1));
    Assertions.assertArrayEquals(
        new byte[] {2}, Main.removeAllOccurrencesKotlin(new byte[] {2}, (byte) 1));
  }

  @Test
  void nameresolvesExpectedVersion() {
    Assertions.assertEquals(System.getProperty("COMMONS_LANG_VERSION"), Main.loadVersion());
  }
}
