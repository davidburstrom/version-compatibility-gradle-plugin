package io.github.davidburstrom.gradle.versioncompatibility;

import javax.annotation.Nonnull;
import org.gradle.api.tasks.testing.Test;

public interface TestTaskConfig extends HasVersionTuple {
  /**
   * Gets the test task to be configured, based on the versions provided through {@link
   * #getVersions()}
   *
   * @return The test task.
   */
  @Nonnull
  Test getTestTask();
}
