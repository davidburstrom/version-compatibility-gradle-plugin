package io.github.davidburstrom.gradle.versioncompatibility;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
    runAndVerifyOutput();
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
    runAndVerifyOutput();
  }

  private void runAndVerifyOutput() {
    GradleRunner runner = GradleRunner.create();
    runner.forwardOutput();
    final String gradleVersion = System.getProperty("GRADLE_VERSION");
    if (gradleVersion != null) {
      runner.withGradleVersion(gradleVersion);
    }
    runner.withPluginClasspath();
    runner.withArguments(":compatibilityTest");
    runner.withProjectDir(projectDir);
    BuildResult result = runner.build();

    // Verify the result
    assertTrue(result.getOutput().contains("Task :compatibilityTestWithDummy0Dot1"));
  }

  @SuppressWarnings("PMD.AvoidFileStream")
  private void writeString(File file, String string) throws IOException {
    try (Writer writer = new FileWriter(file)) {
      writer.write(string);
    }
  }
}
