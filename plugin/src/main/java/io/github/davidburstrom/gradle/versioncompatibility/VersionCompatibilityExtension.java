package io.github.davidburstrom.gradle.versioncompatibility;

import javax.annotation.Nonnull;
import org.gradle.api.Action;

public interface VersionCompatibilityExtension {
  /**
   * Configures which versions to set up compatibility adapters for.
   *
   * @param action The config action.
   */
  void adapters(@Nonnull Action<AdaptersConfig> action);

  /**
   * Configures which compatibility tests to run.
   *
   * @param action The config action.
   */
  void tests(@Nonnull Action<TestsConfig> action);
}
