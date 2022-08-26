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
