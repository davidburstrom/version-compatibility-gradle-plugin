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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Main {
  static final CompatArrayUtils COMPAT_ARRAY_UTILS;
  static final KompatArrayUtils KOMPAT_ARRAY_UTILS;

  static {
    String version = loadVersion();
    final int minor = Integer.parseInt(version.split("\\.", -1)[1]);
    if (minor >= 10) {
      COMPAT_ARRAY_UTILS = new CompatArrayUtils3Dot10();
      KOMPAT_ARRAY_UTILS = new KompatArrayUtils3Dot10();
    } else if (minor >= 5) {
      COMPAT_ARRAY_UTILS = new CompatArrayUtils3Dot5();
      KOMPAT_ARRAY_UTILS = new KompatArrayUtils3Dot5();
    } else {
      COMPAT_ARRAY_UTILS = new CompatArrayUtils3Dot0();
      KOMPAT_ARRAY_UTILS = new KompatArrayUtils3Dot0();
    }
  }

  static String loadVersion() {
    try {
      final Enumeration<URL> resources =
          Main.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
        final Manifest manifest = new Manifest(resources.nextElement().openStream());
        final String implementationTitle =
            manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_TITLE);
        if (implementationTitle != null && implementationTitle.contains("Commons Lang")) {
          return manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    throw new AssertionError("Expected to find commons-lang on the classpath");
  }

  public static byte[] removeAllOccurrences(byte[] array, byte element) {
    return COMPAT_ARRAY_UTILS.removeAllOccurrences(array, element);
  }

  public static byte[] removeAllOccurrencesKotlin(byte[] array, byte element) {
    return COMPAT_ARRAY_UTILS.removeAllOccurrences(array, element);
  }
}
