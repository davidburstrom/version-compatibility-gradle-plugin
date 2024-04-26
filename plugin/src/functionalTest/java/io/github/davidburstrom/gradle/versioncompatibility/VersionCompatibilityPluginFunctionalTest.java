/*
 * Copyright 2022-2024 David Burström
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
package io.github.davidburstrom.gradle.versioncompatibility;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A simple functional test for the 'io.github.davidburstrom.version-compatibility' plugin. */
class VersionCompatibilityPluginFunctionalTest {
  @TempDir File projectDir;

  private File getSettingsFile() {
    return new File(projectDir, "settings.gradle");
  }

  @Test
  void specificCompatibilityTestIsIncludedByLifecycleTaskInGroovy() throws IOException {
    writeString(getSettingsFile(), "");
    // language=groovy
    writeString(
        new File(projectDir, "build.gradle"),
        "plugins {\n"
            + "  id 'java'\n"
            + "  id 'io.github.davidburstrom.version-compatibility'\n"
            + "}\n"
            + "versionCompatibility {\n"
            + "  adapters {\n"
            + "    namespaces.register('') {\n"
            + "      versions = ['1.0']\n"
            + "    }\n"
            + "  }\n"
            + "  tests {\n"
            + "    dimensions.register('dummy') { versions = ['0.1']}\n"
            + "  }\n"
            + "}");

    // Run the build
    runAndVerifyOutput(":testCompatibility");
  }

  @Test
  void specificCompatibilityTestIsIncludedByLifecycleTaskInKotlin() throws IOException {
    writeString(getSettingsFile(), "");
    // language=kotlin
    writeString(
        new File(projectDir, "build.gradle.kts"),
        "plugins {\n"
            + "  java\n"
            + "  id(\"io.github.davidburstrom.version-compatibility\")\n"
            + "}\n"
            + "versionCompatibility {\n"
            + "  adapters {\n"
            + "    namespaces.register(\"\") {\n"
            + "      versions.set(listOf(\"1.0\"))\n"
            + "    }\n"
            + "  }\n"
            + "  tests {\n"
            + "    dimensions.register(\"dummy\") { versions.set(listOf(\"0.1\")) }\n"
            + "  }\n"
            + "}");

    // Run the build
    runAndVerifyOutput(":testCompatibility");
  }

  @Test
  void resourceOutputIsDeclared() throws IOException {
    writeString(getSettingsFile(), "");
    // language=kotlin
    writeString(
        new File(projectDir, "build.gradle.kts"),
        "plugins {\n"
            + "  java\n"
            + "  id(\"io.github.davidburstrom.version-compatibility\")\n"
            + "}\n"
            + "versionCompatibility {\n"
            + "  adapters {\n"
            + "    namespaces.register(\"\") {\n"
            + "      versions.set(listOf(\"1.0\"))\n"
            + "    }\n"
            + "  }\n"
            + "  tests {\n"
            + "    dimensions.register(\"dummy\") { versions.set(listOf(\"0.1\")) }\n"
            + "  }\n"
            + "}");
    writeString(new File(projectDir, "src/test/java/Test.java"), "class Test { }");

    runAndVerifyOutput(":processTestResources", ":testCompatibility");
  }

  private void runAndVerifyOutput(String... arguments) {
    GradleRunner runner = GradleRunner.create();
    runner.forwardOutput();
    final String gradleVersion = System.getProperty("GRADLE_VERSION");
    if (gradleVersion != null) {
      runner.withGradleVersion(gradleVersion);
    }
    runner.withPluginClasspath();
    runner.withArguments(arguments);
    runner.withProjectDir(projectDir);
    BuildResult result = runner.build();

    // Verify the result
    assertTrue(result.getOutput().contains("Task :testCompatibilityWithDummy0Dot1"));
  }

  private void writeString(File file, String string) throws IOException {
    file.getParentFile().mkdirs();
    try (Writer writer = Files.newBufferedWriter(file.toPath(), Charset.defaultCharset())) {
      writer.write(string);
    }
  }
}
